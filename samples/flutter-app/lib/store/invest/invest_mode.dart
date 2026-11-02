import '../../merchant/order_registrar.dart';
import '../model/catalog.dart';

/// The developer settings ask for AFT transfers: the start screen then shows Northwind Invest, where a transfer tops
/// up an account, instead of the stores, where it would look like buying goods. Transfers go through API V1 only.
bool isAftDemo(ApiVersion apiVersion, OrderType orderType) => apiVersion == ApiVersion.v1 && orderType == OrderType.aft;

/// The businesses on the start screen.
List<CoverId> visibleCovers({required bool aftDemo}) =>
    aftDemo ? const [CoverId.invest] : const [CoverId.furniture, CoverId.coffee, CoverId.tickets];

/// Whether [cover] belongs to the mode: a business left open while the settings switched modes would register the other
/// kind of order (a purchase in Northwind, a transfer for a cart).
bool coverFits(CoverId cover, {required bool aftDemo}) => visibleCovers(aftDemo: aftDemo).contains(cover);

/// The business to reopen at launch: the last one, unless the settings have since hidden it.
CoverId? reopenCover(CoverId? last, {required bool aftDemo}) =>
    last != null && coverFits(last, aftDemo: aftDemo) ? last : null;
