import AsyncStorage from '@react-native-async-storage/async-storage';

import { isLanguageCode, type LanguageCode } from '../l10n/l10n';
import { Listenable } from '../listenable';
import { apiVersions, orderTypes, type ApiVersion, type OrderType } from '../merchant/orderRegistrar';
import { randomHex } from '../randomHex';
import { InvestAccount } from '../store/invest/investAccount';
import { coverIds, type CoverId } from '../store/model/catalog';
import { sheetThemeOverrides, type Scenario, type SheetThemeOverride } from '../store/model/scenario';

/** The part of AsyncStorage the settings use; tests pass the library's in-memory mock. */
export type PrefsStorage = Pick<typeof AsyncStorage, 'multiGet' | 'setItem' | 'removeItem'>;

const Keys = {
  baseUrl: 'base_url',
  mdOrder: 'md_order',
  pubKeyPem: 'pub_key_pem',
  bindingId: 'core_bid',
  sdkCoreVariant: 'core_var',
  googlePayGateway: 'gpay_gw',
  googlePayMerchantId: 'gpay_mid',
  applePayMerchantId: 'apay_mid',
  login: 'store_login',
  password: 'store_password',
  apiVersion: 'store_api_version',
  orderType: 'store_order_type',
  apiKey: 'store_api_key',
  clientId: 'store_client_id',
  buyerWithCards: 'store_buyer_cards',
  wallet: 'store_wallet',
  scanner: 'store_scanner',
  sdkResult: 'store_sdk_result',
  lastCover: 'store_last_cover',
  lastMdOrder: 'store_last_md_order',
  sheetTheme: 'store_sheet_theme',
  locale: 'store_locale',
  investAccount: 'invest_account',
} as const;

/**
 * The demo's settings, stored only on the device. They are read once at launch and then served from memory, so a
 * change is seen at once — by the screens, which listen, and by the next payment — while it is being written.
 */
export class AppPrefs extends Listenable {
  static readonly defaultBaseUrl = 'https://dev.bpcbt.com/payment';
  static readonly defaultMdOrder = 'c400b41a-aa3d-43db-8727-ac4ca9e8f701';
  static readonly defaultBindingId = '513b17f4-e32e-414f-8c74-936fd7027baa';
  /** A placeholder: Apple Pay needs the merchant ID of a real Apple developer account. */
  static readonly defaultApplePayMerchantId = 'merchant.net.payrdr.samples.test';
  static readonly defaultPubKeyPem = `-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAij/G3JVV3TqYCFZTPmwi
4JduQMsZ2HcFLBBA9fYAvApv3FtA+zKdUGgKh/OPbtpsxe1C57gIaRclbzMoafTb
0eOdj+jqSEJMlVJYSiZ8Hn6g67evhu9wXh5ZKBQ1RUpqL36LbhYnIrP+TEGR/Vyj
bC6QTfaktcRfa8zRqJczHFsyWxnlfwKLfqKz5wSqXkShcrwcfRJCyDRjZX6OFUEC
HsWVK3WMcOV3WZREwbCkh/o5R5Vl6xoyLvSqVEKQiHupJcZu9UEOJiP3yNCn9YPg
yFs2vrCeg6qxDPFnCfetcDCLjjLenGF7VyZzBJ9G2NP3k/mNVtD8Kl7lpiurwY7E
ZwIDAQAB
-----END PUBLIC KEY-----`;

  /** Reads the stored settings and makes sure there is a demo buyer. */
  static async load(storage: PrefsStorage = AsyncStorage): Promise<AppPrefs> {
    const stored = await storage.multiGet(Object.values(Keys));
    const values = new Map(stored.flatMap(([key, value]) => (value === null ? [] : [[key, value] as const])));
    const prefs = new AppPrefs(storage, values);
    if (!prefs.clientId) await prefs.resetClientId();
    return prefs;
  }

  private constructor(private readonly storage: PrefsStorage, private readonly values: Map<string, string>) {
    super();
  }

  /** Gateway base URL for the SDK and order registration. */
  get baseUrl(): string {
    return this.values.get(Keys.baseUrl) ?? AppPrefs.defaultBaseUrl;
  }

  /** No merchant login or password ships with the app: they are typed in the developer settings. */
  get merchantLogin(): string {
    return this.values.get(Keys.login) ?? '';
  }

  get merchantPassword(): string {
    return this.values.get(Keys.password) ?? '';
  }

  /** The merchant API demo orders are registered through. */
  get apiVersion(): ApiVersion {
    const value = this.values.get(Keys.apiVersion);
    return apiVersions.find(version => version === value) ?? 'V1';
  }

  /** What API V1 registers: a payment or an AFT transfer. */
  get orderType(): OrderType {
    const value = this.values.get(Keys.orderType);
    return orderTypes.find(type => type === value) ?? 'PURCHASE';
  }

  /** No API V2 key ships with the app either. */
  get apiKey(): string {
    return this.values.get(Keys.apiKey) ?? '';
  }

  /** Empty: Google Pay takes its gateway and merchant id from the gateway's settings for the merchant. */
  get googlePayGateway(): string {
    return this.values.get(Keys.googlePayGateway) ?? '';
  }

  get googlePayMerchantId(): string {
    return this.values.get(Keys.googlePayMerchantId) ?? '';
  }

  get applePayMerchantId(): string {
    return this.values.get(Keys.applePayMerchantId) ?? AppPrefs.defaultApplePayMerchantId;
  }

  /** The demo buyer whose cards are saved; the order carries it when the scenario says "With cards". */
  get clientId(): string {
    return this.values.get(Keys.clientId) ?? '';
  }

  get scenario(): Scenario {
    return {
      buyerWithCards: this.flag(Keys.buyerWithCards, false),
      wallet: this.flag(Keys.wallet, true),
      scanner: this.flag(Keys.scanner, true),
      sdkResultScreen: this.flag(Keys.sdkResult, false),
    };
  }

  /** The Northwind Invest demo account; the seed until a transfer or an investment changes it. */
  get investAccount(): InvestAccount {
    return InvestAccount.fromJson(this.values.get(Keys.investAccount));
  }

  /** The business to open on the next launch; undefined opens the start screen. */
  get lastCover(): CoverId | undefined {
    const value = this.values.get(Keys.lastCover);
    return coverIds.find(id => id === value);
  }

  /** mdOrder of the last registered order, shown in the developer settings. */
  get lastMdOrder(): string | undefined {
    return this.values.get(Keys.lastMdOrder);
  }

  get sheetTheme(): SheetThemeOverride {
    const value = this.values.get(Keys.sheetTheme);
    return sheetThemeOverrides.find(theme => theme === value) ?? 'cover';
  }

  /** Language of the demo and the SDK sheet; undefined follows the system. */
  get localeOverride(): LanguageCode | undefined {
    const value = this.values.get(Keys.locale);
    return isLanguageCode(value) ? value : undefined;
  }

  /** The SDK Core form: what was pasted last time. Card data are never stored. */
  get mdOrder(): string {
    return this.values.get(Keys.mdOrder) ?? AppPrefs.defaultMdOrder;
  }

  get pubKeyPem(): string {
    return this.values.get(Keys.pubKeyPem) ?? AppPrefs.defaultPubKeyPem;
  }

  get bindingId(): string {
    return this.values.get(Keys.bindingId) ?? AppPrefs.defaultBindingId;
  }

  get sdkCoreVariant(): string {
    return this.values.get(Keys.sdkCoreVariant) ?? 'new';
  }

  setBaseUrl = (value: string) => this.save({ [Keys.baseUrl]: value });

  /** Stores the merchant login and/or password typed in the developer settings; they never leave the device. */
  setCredentials = ({ login, password }: { login?: string; password?: string }) =>
    this.save({ [Keys.login]: login, [Keys.password]: password });

  /** Chooses the merchant API demo orders are registered through. */
  setApiVersion = (value: ApiVersion) => this.save({ [Keys.apiVersion]: value });

  /** Chooses what API V1 registers: a payment or an AFT transfer. */
  setOrderType = (value: OrderType) => this.save({ [Keys.orderType]: value });

  /** Stores the API V2 key typed in the developer settings; it never leaves the device. */
  setApiKey = (value: string) => this.save({ [Keys.apiKey]: value });

  /** Google Pay gateway name and/or merchant id; undefined leaves a value as it is. */
  setGooglePay = ({ gateway, merchantId }: { gateway?: string; merchantId?: string }) =>
    this.save({ [Keys.googlePayGateway]: gateway, [Keys.googlePayMerchantId]: merchantId });

  setApplePayMerchantId = (value: string) => this.save({ [Keys.applePayMerchantId]: value });

  setScenario = (value: Scenario) =>
    this.save({
      [Keys.buyerWithCards]: `${value.buyerWithCards}`,
      [Keys.wallet]: `${value.wallet}`,
      [Keys.scanner]: `${value.scanner}`,
      [Keys.sdkResult]: `${value.sdkResultScreen}`,
    });

  /** Replaces the demo buyer with a new one, without saved cards. */
  resetClientId = () => this.save({ [Keys.clientId]: newClientId() });

  setLastCover = (value: CoverId | undefined) => this.save({ [Keys.lastCover]: value ?? null });

  /** Saves the Northwind Invest demo account; undefined puts it back as a fresh install has it. */
  setInvestAccount = (value: InvestAccount | undefined) =>
    this.save({ [Keys.investAccount]: value === undefined ? null : InvestAccount.toJson(value) });

  setLastMdOrder = (value: string) => this.save({ [Keys.lastMdOrder]: value });

  setSheetTheme = (value: SheetThemeOverride) => this.save({ [Keys.sheetTheme]: value });

  setLocaleOverride = (code: LanguageCode | undefined) => this.save({ [Keys.locale]: code ?? null });

  setMdOrder = (value: string) => this.save({ [Keys.mdOrder]: value });

  setPubKeyPem = (value: string) => this.save({ [Keys.pubKeyPem]: value });

  setBindingId = (value: string) => this.save({ [Keys.bindingId]: value });

  setSdkCoreVariant = (value: string) => this.save({ [Keys.sdkCoreVariant]: value });

  private flag(key: string, fallback: boolean): boolean {
    const value = this.values.get(key);
    return value === undefined ? fallback : value === 'true';
  }

  /** Saves each value; null removes it, undefined leaves it as it is. */
  private async save(changes: Record<string, string | null | undefined>): Promise<void> {
    const entries = Object.entries(changes).filter((entry): entry is [string, string | null] => entry[1] !== undefined);
    for (const [key, value] of entries) {
      if (value === null) this.values.delete(key);
      else this.values.set(key, value);
    }
    this.notify();
    await Promise.all(
      entries.map(([key, value]) => (value === null ? this.storage.removeItem(key) : this.storage.setItem(key, value))),
    );
  }
}

// Short on purpose: with a 41-character id the test gateway registered the order but declined the payment that
// saves the card (71015). The shape is the start of a UUID, as in the native samples.
function newClientId(): string {
  return `demo-${randomHex(8)}-${randomHex(4)}`;
}
