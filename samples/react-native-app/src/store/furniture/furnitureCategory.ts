import { products, type LocalText, type Product } from '../model/catalog';

/** Furniture categories behind the catalogue chips. */
export type FurnitureCategory = 'all' | 'chairs' | 'sofas' | 'light';

export const furnitureCategories: readonly FurnitureCategory[] = ['all', 'chairs', 'sofas', 'light'];

export const categoryLabel: Record<FurnitureCategory, LocalText> = {
  all: s => s.furnitureAll,
  chairs: s => s.furnitureChairs,
  sofas: s => s.furnitureSofas,
  light: s => s.furnitureLight,
};

/** Products of `category`; `all` keeps every product. */
export function furnitureFilter(category: FurnitureCategory): readonly Product[] {
  return products('furniture').filter(product => category === 'all' || categoryOf(product.id) === category);
}

function categoryOf(id: string): FurnitureCategory {
  switch (id) {
    case 'velvet_sofa':
      return 'sofas';
    case 'arc_lamp':
      return 'light';
    default:
      return 'chairs';
  }
}
