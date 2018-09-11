package com.progetto.user.speedo;


import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

/**
 * Adapter per i tab con i vari fragment, attaccato al ViewPager.
 * Gestisce automaticamente il caching.
 */
class CustomStatePagerAdapter extends FragmentStatePagerAdapter {
    private final static int TAB_NUM = 3;
    private final String[] tabNames;
    /*
        Uso delle WeakReference perché non si sa cosa
         fa l'adapter con i fragment e così mi assicuro che
         possono essere deallocati.
      */
    private final SparseArray<WeakReference<Fragment>> mFragments;

    CustomStatePagerAdapter(FragmentManager fragmentManager, String[] tabNames) {
        super(fragmentManager);
        mFragments = new SparseArray<>();
        this.tabNames = tabNames;
    }

    /**
     * Ritorna una nuova istanza del fragment
     * che sta in posizione position.
     *
     * @param position posizione del fragment da ritornare
     * @return fragment in posizione position
     */
    @Override
    public Fragment getItem(int position) {
        Fragment result = null;
        switch (position) {
            case 0:
                result = StatsFragment.newInstance();
                break;
            case 1:
                result = RunFragment.newInstance();
                break;
            case 2:
                result = SocialFragment.newInstance();
                break;
        }
        return result;
    }

    @Override
    public int getCount() {
        return TAB_NUM;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        if (position < 0 || position > TAB_NUM) return null;
        return tabNames[position];
    }

    @Nonnull
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment f = (Fragment) super.instantiateItem(container, position);
        mFragments.put(position, new WeakReference<>(f));
        return super.instantiateItem(container, position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        mFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    /**
     * Ritorna il fragment in posizione position
     * per poter chiamare i suoi metodi dall'esterno
     *
     * @param position posizione del fragment da ritornare
     * @return fragment in posizione position
     */
    public Fragment getFragment(int position) {
        if (mFragments == null) return null;
        WeakReference<Fragment> weakReference = mFragments.get(position);
        if (weakReference == null) return null;
        return weakReference.get();
    }
}
