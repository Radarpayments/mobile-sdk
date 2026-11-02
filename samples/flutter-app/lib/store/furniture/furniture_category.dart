import '../model/catalog.dart';

/// Furniture categories behind the catalogue chips.
enum FurnitureCategory {
  all,
  chairs,
  sofas,
  light;

  LocalText get label => switch (this) {
    all => (l) => l.furnitureAll,
    chairs => (l) => l.furnitureChairs,
    sofas => (l) => l.furnitureSofas,
    light => (l) => l.furnitureLight,
  };
}

/// Products of [category]; [FurnitureCategory.all] keeps every product.
List<Product> furnitureFilter(FurnitureCategory category) => [
  for (final product in Catalog.products(CoverId.furniture))
    if (category == FurnitureCategory.all || _categoryOf(product.id) == category) product,
];

FurnitureCategory _categoryOf(String id) => switch (id) {
  'velvet_sofa' => FurnitureCategory.sofas,
  'arc_lamp' => FurnitureCategory.light,
  _ => FurnitureCategory.chairs,
};
