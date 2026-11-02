import React, { useEffect } from 'react';
import { Image, ScrollView, StyleSheet, Text, View, type ImageSourcePropType, type TextStyle } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useL10n, usePrefs } from '../AppScope';
import type { ScreenProps } from '../navigation';
import { Icon } from '../ui/Icon';
import { SystemBars } from '../ui/SystemBars';
import { Tap } from '../ui/Tap';
import { InvestTile } from './invest/InvestStore';
import { isAftDemo, reopenCover, visibleCovers } from './invest/investMode';
import { photos, type CoverId, type LocalText } from './model/catalog';
import { coverStyle, font, heading, StartColors } from './theme/coverStyle';
import { photoFill } from '../ui/photoFill';

export const coverName: Record<CoverId, LocalText> = {
  furniture: s => s.coverFurnitureName,
  coffee: s => s.coverCoffeeName,
  tickets: s => s.coverTicketsName,
  invest: s => s.investName,
};

const tiles: Record<Exclude<CoverId, 'invest'>, { image: ImageSourcePropType; tagline: LocalText }> = {
  furniture: { image: photos.furnitureHero, tagline: s => s.coverFurnitureTagline },
  coffee: { image: photos.coffeeHero, tagline: s => s.coverCoffeeTagline },
  tickets: { image: photos.ticketsHero, tagline: s => s.coverTicketsTagline },
};

const stores = visibleCovers(false) as Array<keyof typeof tiles>;

/** First screen: the SDK Payment stores, SDK Core's own card form, and the developer settings. */
export function StartScreen({ navigation }: ScreenProps<'Start'>) {
  const prefs = usePrefs();
  const { strings } = useL10n();
  const aftDemo = isAftDemo(prefs.apiVersion, prefs.orderType);

  /** Opens the business `cover`; it opens again on the next launch until the buyer leaves it. */
  const openCover = (cover: CoverId) => {
    prefs.setLastCover(cover);
    navigation.push('Cover', { cover });
  };

  // The last business opens straight away, once per launch.
  useEffect(() => {
    const last = reopenCover(prefs.lastCover, isAftDemo(prefs.apiVersion, prefs.orderType));
    if (last) navigation.push('Cover', { cover: last });
  }, [navigation, prefs]);

  return (
    <SafeAreaView style={styles.screen}>
      <SystemBars lightBackground />
      {/* At least a screen tall: the store tiles share what the rest leaves, so SDK Core sits at the bottom. On a
          short screen the tiles keep their minimum height and the page scrolls instead. */}
      <ScrollView contentContainerStyle={styles.page}>
        <View style={styles.titleRow}>
          <Text style={styles.title} accessibilityRole="header">
            {strings.startTitle}
          </Text>
          <Tap
            onPress={() => navigation.push('Settings')}
            accessibilityLabel={strings.startSettings}
            testID="start_settings"
            style={styles.gear}
          >
            <Icon name="settings_outlined" color={StartColors.text} />
          </Tap>
        </View>
        {/* AFT orders top up an account: shown in a business where that is what happens, not in the stores. */}
        <Section
          title={strings.startSectionPayment}
          subtitle={aftDemo ? strings.startSectionPaymentAftSub : strings.startSectionPaymentSub}
        />
        <View style={styles.stores}>
          {aftDemo && <InvestTile onPress={() => openCover('invest')} />}
          {stores.filter(() => !aftDemo).map(cover => (
            <PhotoTile
              key={cover}
              testID={`cover_${cover}`}
              image={tiles[cover].image}
              name={coverName[cover](strings)}
              nameStyle={heading(coverStyle(cover), 22)}
              tagline={tiles[cover].tagline(strings)}
              onPress={() => openCover(cover)}
              grow
            />
          ))}
        </View>
        <Section title={strings.startSectionCore} subtitle={strings.startSectionCoreSub} />
        <PhotoTile
          testID="core_tile"
          image={photos.coreHero}
          name={strings.coreTileTitle}
          nameStyle={font('Inter', 22, { weight: 800 })}
          tagline={strings.coreTileSub}
          onPress={() => navigation.push('Core')}
        />
      </ScrollView>
    </SafeAreaView>
  );
}

/** Heading of one SDK on the start screen. */
function Section({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle} accessibilityRole="header">
        {title}
      </Text>
      <Text style={styles.sectionSubtitle}>{subtitle}</Text>
    </View>
  );
}

interface PhotoTileProps {
  image: ImageSourcePropType;
  name: string;
  nameStyle: TextStyle;
  tagline: string;
  onPress: () => void;
  testID: string;
  /** Takes a share of the free height instead of a fixed one. */
  grow?: boolean;
}

/** Photo with a dark fade at the bottom, the name and a tagline over it. */
function PhotoTile({ image, name, nameStyle, tagline, onPress, testID, grow = false }: PhotoTileProps) {
  return (
    <Tap
      onPress={onPress}
      testID={testID}
      accessibilityLabel={`${name}. ${tagline}`}
      rippleColor="#FFFFFF33"
      style={[styles.tile, grow ? styles.tileGrow : styles.tileFixed]}
    >
      {/* Every layer is absolute: the tile's height comes from the layout, never from the photo. */}
      <Image source={image} resizeMode="cover" style={photoFill} />
      <View style={[StyleSheet.absoluteFill, styles.tileFade]} />
      <View style={styles.tileText}>
        <Text style={[nameStyle, styles.white]}>{name}</Text>
        <Text style={styles.tagline}>{tagline}</Text>
      </View>
    </Tap>
  );
}

const TILE_HEIGHT = 112;

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: StartColors.background },
  page: { flexGrow: 1, paddingHorizontal: 16, paddingBottom: 16 },
  titleRow: { flexDirection: 'row', alignItems: 'center', paddingTop: 8 },
  title: { flex: 1, ...font('Inter', 26, { weight: 800, color: StartColors.text }) },
  gear: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  section: { paddingTop: 20, paddingBottom: 10, gap: 2 },
  sectionTitle: font('Inter', 17, { weight: 800, color: StartColors.text }),
  sectionSubtitle: font('Inter', 13, { lineHeight: 18, color: StartColors.muted }),
  stores: { flexGrow: 1, gap: 10 },
  tile: { borderRadius: 20, overflow: 'hidden', backgroundColor: StartColors.border },
  tileGrow: { flexGrow: 1, flexBasis: 0, minHeight: TILE_HEIGHT },
  tileFixed: { height: TILE_HEIGHT },
  tileFade: { backgroundImage: 'linear-gradient(to bottom, transparent 30%, rgba(0, 0, 0, 0.68) 100%)' },
  tileText: { position: 'absolute', start: 16, end: 16, bottom: 16 },
  white: { color: '#FFFFFF' },
  tagline: font('Inter', 13, { color: 'rgba(255, 255, 255, 0.92)' }),
});
