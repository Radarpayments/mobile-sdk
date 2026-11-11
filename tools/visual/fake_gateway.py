#!/usr/bin/env python3
"""A fake payment gateway for the visual tests of the iOS sample: fixed answers, no card data kept or logged.

    python3 tools/visual/fake_gateway.py --port 8787        # the app's base URL: http://127.0.0.1:8787/payment

The scenario of an order is the merchant login it was registered with: tokens joined by dots, e.g.
`cards3.wallet.cvc.decline`. `GET /__scenario/<name>` sets a scenario that overrides the login until
`GET /__scenario/-` clears it; `GET /__health` answers `ok`; `GET /__release/all` ends the delays in progress.

Tokens:
    cards0 | cards1 | cards3 | cards8   saved cards of the buyer (default cards0)
    cvc                                 a saved card asks for its CVC
    nodelete                            saved cards cannot be removed
    binding                             "save card" is offered
    wallet                              Apple Pay is allowed for the merchant
    payer_contact | payer_billing       the gateway demands email + phone / those and the billing address
    decline                             the payment is declined with a gateway message
    threeds                             the payment goes through a 3-D Secure page served here
    expired                             the order's session is over
    slow_session | slow_settings | slow_keys | slow_process | slow_finish
                                        that call answers after --slow seconds (default 30)
An order registered with registerP2P.do is an AFT transfer (P2P, WITHOUT_TO_CARD); its saved cards are debit ones.
"""

import argparse
import json
import threading
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, quote, urlparse

# The test-only RSA key of sdk-payment's commonTest Fixtures: nobody holds its private half.
PUBLIC_KEY = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvjG7qd50WLuBmw7rUpQo
xN44zA/XUddLxYAN22fJE9GbLMwKcT0fVE//KjAUDDy3s8p4QuNuxLfUsINM4DlV
IbQZx3ZOXUaQILYCqmqqvbibEk4ulITdMS6QjEsHXdcKXpr+1ZyVG90RI7oxyQf8
YKTxqOQF7lYqicEb6ucPJwYxLA1IoPu9K5PAysLrFiEwuq+z6dNFrvVMXSDAefry
Hc/MnjXhGLc3qcfH9sJczKQ8we6I7zIDgRcIjsZ8xI8ME+X36UJpYPvrU41Xssi1
6MCObHbZpZTEjShPnfmwTIDMbjnqwitXKaC0C+vJ+DvnL5+iqsHN+HT+z8NcQXco
xQIDAQAB
-----END PUBLIC KEY-----"""

CARDS = [
    ("411111**1111 12/30", "VISA"),
    ("555555**4444 08/29", "MASTERCARD"),
    ("378282**0005 03/31", "AMEX"),
    ("400000**0002 01/28", "VISA"),
    ("222300**0023 11/29", "MASTERCARD"),
    ("601100**1117 06/30", "DISCOVER"),
    ("353011**0000 09/28", "JCB"),
    ("620000**0005 02/31", "CUP"),
]
CONTACT = ["EMAIL", "MOBILE_PHONE"]
BILLING = CONTACT + [
    "BILLING_COUNTRY", "BILLING_STATE", "BILLING_CITY", "BILLING_POSTAL_CODE", "BILLING_ADDRESS_LINE1",
]
PAYMENT_SYSTEMS = ["VISA", "MASTERCARD", "AMEX", "DISCOVER", "JCB", "CUP", "MAESTRO"]
DECLINE_MESSAGE = "Insufficient funds on the card"
PAYMENT_DATE = "2026-10-22 14:04:07"
PAY_CALLS = (
    "/rest/processform.do", "/rest/processBindingForm.do", "/applepay/paymentOrder.do", "/google/paymentOrder.do",
    "/rest/p2p/perform.do", "/rest/p2p/performByBinding.do",
)

ACS_PAGE = """<!doctype html>
<html><head><meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  body { font: 17px -apple-system, sans-serif; margin: 0; padding: 32px 24px; background: #f4f6f8; color: #111; }
  h1 { font-size: 22px; margin: 0 0 8px; }
  p { color: #555; margin: 0 0 24px; }
  a { display: block; text-align: center; padding: 14px; border-radius: 12px; text-decoration: none;
      background: #0a5cff; color: #fff; font-weight: 600; margin-bottom: 12px; }
  a.fail { background: #fff; color: #c0392b; border: 1px solid #c0392b; }
</style></head>
<body>
  <h1>Fake Bank</h1>
  <p>Confirm the payment to finish the 3-D Secure check.</p>
  <a id="confirm" href="sdk://done?orderId=%(order)s">Confirm payment</a>
  <a id="reject" class="fail" href="sdk://done?orderId=%(order)s&error=declined">Reject</a>
</body></html>"""


class Gateway:
    def __init__(self, slow):
        self.slow = slow
        self.lock = threading.Lock()
        self.orders = {}
        self.forced = None
        self.released = threading.Event()

    def register(self, login, amount_minor, transfer, features):
        md_order = str(uuid.uuid4())
        with self.lock:
            scenario = self.forced or login or ""
            self.orders[md_order] = {
                "tokens": set(t for t in scenario.split(".") if t),
                "login": login or "merchant",
                "amount": int(amount_minor or 0),
                "number": "001000",
                "transfer": transfer,
                "features": features,
                "removed": set(),
                "paid": None,
            }
        return md_order

    def order(self, md_order):
        with self.lock:
            return self.orders.get(md_order or "")

    def open_order_has(self, token):
        with self.lock:
            return any(token in o["tokens"] and o["paid"] is None for o in self.orders.values())

    def hold(self):
        self.released.clear()
        self.released.wait(self.slow)


def amount_text(order):
    return "%d.%02d" % divmod(order["amount"], 100)


def bindings(order):
    count = next((int(t[5:]) for t in order["tokens"] if t.startswith("cards") and t[5:].isdigit()), 0)
    items = []
    for index, (label, system) in enumerate(CARDS[:count]):
        binding_id = "binding-%d" % (index + 1)
        if binding_id in order["removed"]:
            continue
        item = {"id": binding_id, "label": label, "paymentSystem": system, "cardHolder": "ALEX MORGAN",
                "createdDate": 1767225600000 + index}
        if order["transfer"]:
            item["bindingCategory"] = "D"
        items.append(item)
    return items


def session_status(order):
    tokens = order["tokens"]
    if "expired" in tokens:
        return {"orderNumber": order["number"]}
    options = []
    if "wallet" in tokens:
        options.append("APPLEPAY")
    if order["transfer"]:
        options.append("DEBIT_BINDING_ALLOWED")
    fields = BILLING if "payer_billing" in tokens else CONTACT if "payer_contact" in tokens else []
    saving = "binding" in tokens or any(t.startswith("cards") and t != "cards0" for t in tokens)
    body = {
        "remainingSecs": 1200,
        "orderNumber": order["number"],
        "amount": amount_text(order),
        "currencyAlphaCode": "EUR",
        "bindingItems": bindings(order),
        "cvcNotRequired": "cvc" not in tokens,
        "cvcBindingNotRequired": "cvc" not in tokens,
        "otherWayEnabled": True,
        "bindingEnabled": saving,
        "bindingDeactivationEnabled": "nodelete" not in tokens,
        "feeEnabled": False,
        "orderExpired": False,
        "backUrl": "sdk://done",
        "merchantInfo": {
            "merchantLogin": order["login"],
            "merchantFullName": "Visual Test Store",
            "merchantCountry": {"countryCode": "196", "countryCodeAlpha2": "CY"},
        },
        "merchantOptions": options,
        "customerDetails": {"suggestedCountryCodes": [{"phoneCountryCode": "357", "alpha2": "CY"}]},
        "orderFeatures": order["features"],
    }
    if fields:
        params = [{"name": name, "mandatory": True, "editable": True} for name in fields]
        body["payerDataParamsNeedToBeFilled"] = {system: params for system in PAYMENT_SYSTEMS}
    if order["transfer"]:
        body["orderPaymentWay"] = "P2P"
    return body


def finished_info(order):
    if "expired" in order["tokens"]:
        return {"sessionExpired": True}
    info = {"formattedAmount": amount_text(order), "orderFeatures": order["features"]}
    if order["paid"] is None:
        info.update(status="CREATED", actionCode=-100)
    elif order["paid"]:
        info.update(status="DEPOSITED", actionCode=0, actionCodeDescription="Request processed successfully",
                    paymentDate=PAYMENT_DATE, panMasked="411111**1111", approvalCode="123456", refNum="111111111111",
                    terminalId="10000001")
    else:
        info.update(status="DECLINED", actionCode=116, actionCodeDescription="Not sufficient funds",
                    displayErrorMessage=DECLINE_MESSAGE, errorTypeName="INSUFFICIENT_FUNDS",
                    paymentDate=PAYMENT_DATE, panMasked="411111**1111")
    return info


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    gateway = None

    # Bodies carry card cryptograms and payer data: only the method and the path are ever printed.
    def log_message(self, fmt, *args):
        print("%s %s" % (self.command, urlparse(self.path).path), flush=True)

    def send(self, body, content_type="application/json", status=200):
        data = body.encode() if isinstance(body, str) else json.dumps(body).encode()
        self.send_response(status)
        self.send_header("Content-Type", content_type + "; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        try:
            self.wfile.write(data)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def params(self):
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length).decode("utf-8", "replace") if length else ""
        query = {k: v[0] for k, v in parse_qs(urlparse(self.path).query).items()}
        if "json" in (self.headers.get("Content-Type") or ""):
            try:
                parsed = json.loads(raw or "{}")
            except ValueError:
                parsed = {}
            return {**query, **(parsed if isinstance(parsed, dict) else {})}
        return {**query, **{k: v[0] for k, v in parse_qs(raw, keep_blank_values=True).items()}}

    def do_GET(self):
        self.route()

    def do_POST(self):
        self.route()

    def route(self):
        gw = self.gateway
        path = urlparse(self.path).path
        p = self.params()
        if path == "/__health":
            return self.send("ok", "text/plain")
        if path.startswith("/__scenario/"):
            name = path.rsplit("/", 1)[1]
            with gw.lock:
                gw.forced = None if name in ("", "-") else name
            return self.send("ok", "text/plain")
        if path.startswith("/__release/"):
            gw.released.set()
            return self.send("ok", "text/plain")
        if path == "/acs":
            return self.send(ACS_PAGE % {"order": quote(str(p.get("MD", "")))}, "text/html")
        if not path.startswith("/payment/"):
            return self.send({"error": "Not found"}, status=404)
        call = path[len("/payment"):]

        if call == "/rest/register.do":
            return self.send({"orderId": gw.register(p.get("userName"), p.get("amount"), False, []),
                              "formUrl": "sdk://done"})
        if call == "/rest/api/p2p/registerP2P.do":
            features = (p.get("features") or {}).get("feature") or ["WITHOUT_TO_CARD"]
            return self.send({"orderId": gw.register(p.get("username"), p.get("amount"), True, features),
                              "formUrl": "sdk://done", "errorCode": 0})
        if call == "/se/keys.do":
            if gw.open_order_has("slow_keys"):
                gw.hold()
            return self.send({"keys": [{"keyValue": PUBLIC_KEY, "protocolVersion": "RSA",
                                        "keyExpiration": 4102444800000}]})
        if call == "/rest/getPaymentSettings.do":
            if gw.open_order_has("slow_settings"):
                gw.hold()
            return self.send({"settings": {}})

        md_order = p.get("MDORDER") or p.get("mdOrder") or p.get("orderId")
        order = gw.order(md_order)
        if order is None:
            return self.send({"errorCode": 6, "errorMessage": "Order not found", "error": "Order not found"})
        tokens = order["tokens"]
        if call == "/rest/getSessionStatus.do":
            if "slow_session" in tokens:
                gw.hold()
            return self.send(session_status(order))
        if call in PAY_CALLS:
            if "slow_process" in tokens:
                gw.hold()
            order["paid"] = "decline" not in tokens
            if "threeds" in tokens:
                host = self.headers.get("Host") or "127.0.0.1"
                return self.send({"errorCode": 0, "acsUrl": "http://%s/acs" % host, "paReq": "fake-pareq",
                                  "termUrl": "http://%s/payment/rest/finish3ds.do?orderId=%s" % (host, md_order)})
            return self.send({"errorCode": 0, "redirect": "sdk://done", "info": "ok"})
        if call == "/rest/getFinishedPaymentInfo.do":
            if "slow_finish" in tokens:
                gw.hold()
            return self.send(finished_info(order))
        if call == "/rest/unbindcardanon.do":
            order["removed"].add(p.get("bindingId"))
            return self.send({"errorCode": 0})
        return self.send({"error": "Not found"}, status=404)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--port", type=int, default=8787)
    parser.add_argument("--slow", type=float, default=30.0, help="seconds a slow_* call takes")
    args = parser.parse_args()
    Handler.gateway = Gateway(args.slow)
    server = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    server.daemon_threads = True
    print("fake gateway on http://127.0.0.1:%d/payment" % args.port, flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
