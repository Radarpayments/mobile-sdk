import React, { useEffect } from 'react';

import { useAppScope, usePrefs } from '../AppScope';
import { useIsTop, type OnOutcome, type ScreenProps } from '../navigation';
import { CoffeeStore } from './coffee/CoffeeStore';
import { FurnitureStore } from './furniture/FurnitureStore';
import { coverFits, isAftDemo } from './invest/investMode';
import { InvestStore } from './invest/InvestStore';
import { TicketsStore } from './tickets/TicketsStore';

/** What every store gets from the app: where a finished payment goes, and the way to the developer settings. */
export interface StoreProps {
  onOutcome: OnOutcome;
  onOpenSettings: () => void;
}

/** The screens of one business; each cover's own flow plugs in here. */
export function CoverScreen({ navigation, route }: ScreenProps<'Cover'>) {
  const { cover } = route.params;
  const { prefs, store } = useAppScope();

  // The screen leaves the stack only when the buyer leaves the business: the next launch opens the start screen.
  useEffect(
    () => () => {
      prefs.setLastCover(undefined);
    },
    [prefs],
  );

  // Back from the settings in the other mode: this business no longer fits it, the start screen shows which do.
  const { apiVersion, orderType } = usePrefs();
  const fits = coverFits(cover, isAftDemo(apiVersion, orderType));
  const top = useIsTop();
  useEffect(() => {
    if (top && !fits) navigation.leave();
  }, [top, fits, navigation]);

  const onOutcome: OnOutcome = (outcome, amountMinor) => {
    store.onOutcome(cover, outcome, amountMinor);
    const receipt = store.lastReceipt;
    if (receipt) navigation.push('StoreResult', { cover, receipt, orderNumber: store.lastOrderNumber });
  };
  const props: StoreProps = { onOutcome, onOpenSettings: () => navigation.push('Settings') };

  switch (cover) {
    case 'furniture':
      return <FurnitureStore {...props} />;
    case 'coffee':
      return <CoffeeStore {...props} />;
    case 'tickets':
      return <TicketsStore {...props} />;
    case 'invest':
      return <InvestStore onBack={() => navigation.goBack()} onOpenSettings={props.onOpenSettings} />;
  }
}
