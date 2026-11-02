import 'dart:async';

import 'package:flutter/material.dart';

import '../../app.dart';
import '../../app_scope.dart';
import '../model/cart.dart';
import '../model/catalog.dart';
import '../store_checkout_bar.dart';
import '../theme/cover_style.dart';
import 'ticket_order.dart';
import 'tickets_event_screen.dart' show holdColour;

/// Checkout: the event, the bill, a seat hold that runs out, and the shared payment bar.
class TicketsCheckoutScreen extends StatefulWidget {
  const TicketsCheckoutScreen({
    super.key,
    required this.order,
    required this.onBack,
    required this.onHoldExpired,
    required this.onOutcome,
    required this.onOpenSettings,
  });

  final TicketOrder order;
  final VoidCallback onBack;
  final VoidCallback onHoldExpired;
  final OnOutcome onOutcome;
  final VoidCallback onOpenSettings;

  @override
  State<TicketsCheckoutScreen> createState() => _TicketsCheckoutScreenState();
}

class _TicketsCheckoutScreenState extends State<TicketsCheckoutScreen> {
  static const _holdSeconds = 600;

  int _secondsLeft = _holdSeconds;
  late final Timer _hold;

  @override
  void initState() {
    super.initState();
    _hold = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() => _secondsLeft--);
      if (_secondsLeft == 0) {
        timer.cancel();
        widget.onHoldExpired();
      }
    });
  }

  @override
  void dispose() {
    _hold.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.tickets);
    final l10n = context.l10n;
    final cart = widget.order.toCart();
    final [seats, fee] = cart.lines;
    final time = '${_secondsLeft ~/ 60}:${(_secondsLeft % 60).toString().padLeft(2, '0')}';
    return SystemBarIcons(
      lightBackground: true,
      child: Scaffold(
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Row(
                    children: [
                      BackButton(onPressed: widget.onBack, color: style.text),
                      Expanded(
                        child: Text(l10n.ticketsCheckout, style: style.heading(22, weight: FontWeight.w800)),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: ListView(
                    children: [
                      _EventCard(seats: seats),
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        child: Column(
                          children: [
                            _Line(
                              l10n.ticketsSeats,
                              context.eur(seats.product.priceMinor * seats.quantity),
                              bold: false,
                            ),
                            const SizedBox(height: 6),
                            _Line(fee.product.name(l10n), context.eur(fee.product.priceMinor), bold: false),
                            const SizedBox(height: 6),
                            _Line(l10n.storeTotal, context.eur(cart.totalMinor), bold: true),
                          ],
                        ),
                      ),
                      Row(
                        children: [
                          const Icon(Icons.schedule, color: holdColour, size: 18),
                          const SizedBox(width: 6),
                          Text(
                            l10n.ticketsHold(time),
                            style: style.body(14, weight: FontWeight.w600, color: holdColour),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  child: StoreCheckoutBar(
                    cover: CoverId.tickets,
                    amountMinor: cart.totalMinor,
                    onOutcome: widget.onOutcome,
                    onOpenSettings: widget.onOpenSettings,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Line extends StatelessWidget {
  const _Line(this.label, this.value, {required this.bold});

  final String label;
  final String value;
  final bool bold;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.tickets);
    final text = style.body(
      bold ? 17 : 14,
      weight: bold ? FontWeight.w800 : FontWeight.w400,
      color: bold ? style.text : style.muted,
    );
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: text),
        Text(value, style: text),
      ],
    );
  }
}

class _EventCard extends StatelessWidget {
  const _EventCard({required this.seats});

  final CartLine seats;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.tickets);
    final l10n = context.l10n;
    return Container(
      margin: const EdgeInsets.only(top: 8),
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        border: Border.all(color: style.border),
        borderRadius: BorderRadius.circular(style.radius),
      ),
      child: Row(
        children: [
          Image.asset(photo('tickets_crowd'), width: 96, height: 96, fit: BoxFit.cover),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(l10n.ticketsTitle.replaceAll('\n', ' '), style: style.heading(15, weight: FontWeight.w800)),
                  Text(l10n.ticketsEventLine(seats.product.name(l10n)), style: style.body(13, color: style.muted)),
                  Text(
                    '${seats.quantity} × ${context.eur(seats.product.priceMinor)}',
                    style: style.body(13, color: style.muted),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
