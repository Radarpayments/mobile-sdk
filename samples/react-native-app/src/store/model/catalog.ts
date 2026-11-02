import type { ImageSourcePropType } from 'react-native';

import type { Strings } from '../../l10n/l10n';

/** The demo businesses. Every cover runs the same payment path and scenarios. */
export type CoverId = 'furniture' | 'coffee' | 'tickets' | 'invest';

export const coverIds: readonly CoverId[] = ['furniture', 'coffee', 'tickets', 'invest'];

/** A text of the demo in the current language. */
export type LocalText = (strings: Strings) => string;

/** A thing the buyer can put in the cart; `priceMinor` is in euro cents. */
export interface Product {
  id: string;
  name: LocalText;
  subtitle?: LocalText;
  priceMinor: number;
  image?: ImageSourcePropType;
}

/** Photos of the demo, bundled by Metro. */
export const photos = {
  coffeeCappuccino: require('../../assets/images/coffee_cappuccino.webp'),
  coffeeCroissant: require('../../assets/images/coffee_croissant.webp'),
  coffeeFlatWhite: require('../../assets/images/coffee_flat_white.webp'),
  coffeeHero: require('../../assets/images/coffee_hero.webp'),
  coreHero: require('../../assets/images/core_hero.webp'),
  furnitureCloudChair: require('../../assets/images/furniture_cloud_chair.webp'),
  furnitureHero: require('../../assets/images/furniture_hero.webp'),
  furnitureLamp: require('../../assets/images/furniture_lamp.webp'),
  furnitureOnoChair: require('../../assets/images/furniture_ono_chair.webp'),
  furnitureSofa: require('../../assets/images/furniture_sofa.webp'),
  furnitureStool: require('../../assets/images/furniture_stool.webp'),
  ticketsCrowd: require('../../assets/images/tickets_crowd.webp'),
  ticketsHero: require('../../assets/images/tickets_hero.webp'),
} satisfies Record<string, ImageSourcePropType>;

/** Id of the per-order service fee line in the tickets cover. */
export const ticketFeeId = 'fee';

// What each demo business sells, in display order. Prices are euro cents.
const catalog: Record<CoverId, readonly Product[]> = {
  furniture: [
    {
      id: 'velvet_sofa',
      name: s => s.productVelvetSofa,
      subtitle: s => s.productVelvetSofaSub,
      priceMinor: 124900,
      image: photos.furnitureSofa,
    },
    {
      id: 'cloud_chair',
      name: s => s.productCloudChair,
      subtitle: s => s.productCloudChairSub,
      priceMinor: 32900,
      image: photos.furnitureCloudChair,
    },
    {
      id: 'ono_chair',
      name: s => s.productOnoChair,
      subtitle: s => s.productOnoChairSub,
      priceMinor: 18900,
      image: photos.furnitureOnoChair,
    },
    {
      id: 'birch_stool',
      name: s => s.productBirchStool,
      subtitle: s => s.productBirchStoolSub,
      priceMinor: 9900,
      image: photos.furnitureStool,
    },
    {
      id: 'arc_lamp',
      name: s => s.productArcLamp,
      subtitle: s => s.productArcLampSub,
      priceMinor: 21900,
      image: photos.furnitureLamp,
    },
    {
      id: 'lume_armchair',
      name: s => s.productLumeArmchair,
      subtitle: s => s.productLumeArmchairSub,
      priceMinor: 45900,
      image: photos.furnitureHero,
    },
  ],
  coffee: [
    {
      id: 'flat_white',
      name: s => s.productFlatWhite,
      subtitle: s => s.productFlatWhiteSub,
      priceMinor: 390,
      image: photos.coffeeFlatWhite,
    },
    {
      id: 'cappuccino',
      name: s => s.productCappuccino,
      subtitle: s => s.productCappuccinoSub,
      priceMinor: 360,
      image: photos.coffeeCappuccino,
    },
    {
      id: 'espresso',
      name: s => s.productEspresso,
      subtitle: s => s.productEspressoSub,
      priceMinor: 260,
      image: photos.coffeeHero,
    },
    {
      id: 'iced_latte',
      name: s => s.productIcedLatte,
      subtitle: s => s.productIcedLatteSub,
      priceMinor: 420,
      image: photos.coffeeHero,
    },
    {
      id: 'croissant',
      name: s => s.productCroissant,
      subtitle: s => s.productCroissantSub,
      priceMinor: 350,
      image: photos.coffeeCroissant,
    },
  ],
  tickets: [
    { id: 'floor', name: s => s.ticketFloor, subtitle: s => s.ticketFloorSub, priceMinor: 4500 },
    { id: 'balcony', name: s => s.ticketBalcony, subtitle: s => s.ticketBalconySub, priceMinor: 6500 },
    { id: ticketFeeId, name: s => s.ticketFee, priceMinor: 350 },
  ],
  // Northwind Invest sells nothing: the buyer tops up an account.
  invest: [],
};

/** Products of `cover`, in display order. */
export function products(cover: CoverId): readonly Product[] {
  return catalog[cover];
}

/** The product `id` of `cover`. */
export function product(cover: CoverId, id: string): Product {
  const found = catalog[cover].find(item => item.id === id);
  if (!found) throw new Error(`No product ${id} in ${cover}`);
  return found;
}
