import type { ApiVersion, OrderType } from '../../merchant/orderRegistrar';
import type { CoverId } from '../model/catalog';

/**
 * The developer settings ask for AFT transfers: the start screen then shows Northwind Invest, where a transfer tops
 * up an account, instead of the stores, where it would look like buying goods. Transfers go through API V1 only.
 */
export function isAftDemo(apiVersion: ApiVersion, orderType: OrderType): boolean {
  return apiVersion === 'V1' && orderType === 'AFT';
}

/** The businesses on the start screen. */
export function visibleCovers(aftDemo: boolean): CoverId[] {
  return aftDemo ? ['invest'] : ['furniture', 'coffee', 'tickets'];
}

/**
 * Whether `cover` belongs to the mode: a business left open while the settings switched modes would register the other
 * kind of order (a purchase in Northwind, a transfer for a cart).
 */
export function coverFits(cover: CoverId, aftDemo: boolean): boolean {
  return visibleCovers(aftDemo).includes(cover);
}

/** The business to reopen at launch: the last one, unless the settings have since hidden it. */
export function reopenCover(last: CoverId | undefined, aftDemo: boolean): CoverId | undefined {
  return last !== undefined && coverFits(last, aftDemo) ? last : undefined;
}
