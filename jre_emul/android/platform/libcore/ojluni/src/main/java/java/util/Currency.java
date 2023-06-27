/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.util;

import com.google.j2objc.LibraryNotLinkedError;
import com.google.j2objc.util.CurrencyNumericCodes;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import sun.util.logging.PlatformLogger;

import libcore.icu.ICU;
import libcore.icu.LocaleData;

// BEGIN Android-changed: Removed docs about superseding runtime currency data.
// Doing so via a properties file is not supported on Android.
/**
 * Represents a currency. Currencies are identified by their ISO 4217 currency
 * codes. Visit the <a href="http://www.iso.org/iso/home/standards/currency_codes.htm">
 * ISO web site</a> for more information.
 * <p>
 * The class is designed so that there's never more than one
 * <code>Currency</code> instance for any given currency. Therefore, there's
 * no public constructor. You obtain a <code>Currency</code> instance using
 * the <code>getInstance</code> methods.
 *
 * <p>
 * It is recommended to use {@link java.math.BigDecimal} class while dealing
 * with {@code Currency} or monetary values as it provides better handling of floating
 * point numbers and their operations.
 *
 * @see java.math.BigDecimal
 * @since 1.4
 */
// END Android-changed: Removed docs about superseding runtime currency data.
public final class Currency implements Serializable {

    private static final long serialVersionUID = -158308464356906721L;

    private static final HashMap<String, Currency> codesToCurrencies = new HashMap<String, Currency>();
    private static final HashMap<Locale, Currency> localesToCurrencies = new HashMap<Locale, Currency>();

    private static final LinkedHashSet<String> availableCurrencyCodes =
        constructAvailableCurrencyCodes();

    /**
     * ISO 4217 currency code for this currency.
     *
     * @serial
     */
    private final String currencyCode;

    // BEGIN Android-changed: Use ICU.
    // We do not keep track of defaultFractionDigits and numericCode separately.
    /*
    /**
     * Default fraction digits for this currency.
     * Set from currency data tables.
     *
    private final transient int defaultFractionDigits;
     */

    /*
     * ISO 4217 numeric code for this currency.
     * Set from currency data tables.
     *
     private final transient int numericCode;
     */
    // private transient final android.icu.util.Currency icuCurrency;
    // END Android-changed: Use ICU.


    // class data: instance map

    private static ConcurrentMap<String, Currency> instances = new ConcurrentHashMap<>(7);
    private static HashSet<Currency> available;

    // BEGIN Android-removed: Use ICU.
    // We don't need any of these static fields nor the static initializer.
    /*
    // Class data: currency data obtained from currency.data file.
    // Purpose:
    // - determine valid country codes
    // - determine valid currency codes
    // - map country codes to currency codes
    // - obtain default fraction digits for currency codes
    //
    // sc = special case; dfd = default fraction digits
    // Simple countries are those where the country code is a prefix of the
    // currency code, and there are no known plans to change the currency.
    //
    // table formats:
    // - mainTable:
    //   - maps country code to 32-bit int
    //   - 26*26 entries, corresponding to [A-Z]*[A-Z]
    //   - \u007F -> not valid country
    //   - bits 20-31: unused
    //   - bits 10-19: numeric code (0 to 1023)
    //   - bit 9: 1 - special case, bits 0-4 indicate which one
    //            0 - simple country, bits 0-4 indicate final char of currency code
    //   - bits 5-8: fraction digits for simple countries, 0 for special cases
    //   - bits 0-4: final char for currency code for simple country, or ID of special case
    // - special case IDs:
    //   - 0: country has no currency
    //   - other: index into specialCasesList

    static int formatVersion;
    static int dataVersion;
    static int[] mainTable;
    static List<SpecialCaseEntry> specialCasesList;
    static List<OtherCurrencyEntry> otherCurrenciesList;

    // handy constants - must match definitions in GenerateCurrencyData
    // magic number
    private static final int MAGIC_NUMBER = 0x43757244;
    // number of characters from A to Z
    private static final int A_TO_Z = ('Z' - 'A') + 1;
    // entry for invalid country codes
    private static final int INVALID_COUNTRY_ENTRY = 0x0000007F;
    // entry for countries without currency
    private static final int COUNTRY_WITHOUT_CURRENCY_ENTRY = 0x00000200;
    // mask for simple case country entries
    private static final int SIMPLE_CASE_COUNTRY_MASK = 0x00000000;
    // mask for simple case country entry final character
    private static final int SIMPLE_CASE_COUNTRY_FINAL_CHAR_MASK = 0x0000001F;
    // mask for simple case country entry default currency digits
    private static final int SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_MASK = 0x000001E0;
    // shift count for simple case country entry default currency digits
    private static final int SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT = 5;
    // maximum number for simple case country entry default currency digits
    private static final int SIMPLE_CASE_COUNTRY_MAX_DEFAULT_DIGITS = 9;
    // mask for special case country entries
    private static final int SPECIAL_CASE_COUNTRY_MASK = 0x00000200;
    // mask for special case country index
    private static final int SPECIAL_CASE_COUNTRY_INDEX_MASK = 0x0000001F;
    // delta from entry index component in main table to index into special case tables
    private static final int SPECIAL_CASE_COUNTRY_INDEX_DELTA = 1;
    // mask for distinguishing simple and special case countries
    private static final int COUNTRY_TYPE_MASK = SIMPLE_CASE_COUNTRY_MASK | SPECIAL_CASE_COUNTRY_MASK;
    // mask for the numeric code of the currency
    private static final int NUMERIC_CODE_MASK = 0x000FFC00;
    // shift count for the numeric code of the currency
    private static final int NUMERIC_CODE_SHIFT = 10;

    // Currency data format version
    private static final int VALID_FORMAT_VERSION = 3;

    static {
        AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public Void run() {
                try {
                    try (InputStream in = getClass().getResourceAsStream("/java/util/currency.data")) {
                        if (in == null) {
                            throw new InternalError("Currency data not found");
                        }
                        DataInputStream dis = new DataInputStream(new BufferedInputStream(in));
                        if (dis.readInt() != MAGIC_NUMBER) {
                            throw new InternalError("Currency data is possibly corrupted");
                        }
                        formatVersion = dis.readInt();
                        if (formatVersion != VALID_FORMAT_VERSION) {
                            throw new InternalError("Currency data format is incorrect");
                        }
                        dataVersion = dis.readInt();
                        mainTable = readIntArray(dis, A_TO_Z * A_TO_Z);
                        int scCount = dis.readInt();
                        specialCasesList = readSpecialCases(dis, scCount);
                        int ocCount = dis.readInt();
                        otherCurrenciesList = readOtherCurrencies(dis, ocCount);
                    }
                } catch (IOException e) {
                    throw new InternalError(e);
                }

                // look for the properties file for overrides
                String propsFile = System.getProperty("java.util.currency.data");
                if (propsFile == null) {
                    propsFile = StaticProperty.javaHome() + File.separator + "lib" +
                        File.separator + "currency.properties";
                }
                try {
                    File propFile = new File(propsFile);
                    if (propFile.exists()) {
                        Properties props = new Properties();
                        try (FileReader fr = new FileReader(propFile)) {
                            props.load(fr);
                        }
                        Pattern propertiesPattern =
                                Pattern.compile("([A-Z]{3})\\s*,\\s*(\\d{3})\\s*,\\s*" +
                                        "(\\d+)\\s*,?\\s*(\\d{4}-\\d{2}-\\d{2}T\\d{2}:" +
                                        "\\d{2}:\\d{2})?");
                        List<CurrencyProperty> currencyEntries
                                = getValidCurrencyData(props, propertiesPattern);
                        currencyEntries.forEach(Currency::replaceCurrencyData);
                    }
                } catch (IOException e) {
                    CurrencyProperty.info("currency.properties is ignored"
                            + " because of an IOException", e);
                }
                return null;
            }
        });
    }

    /**
     * Constants for retrieving localized names from the name providers.
     *
    private static final int SYMBOL = 0;
    private static final int DISPLAYNAME = 1;
    */
    // END Android-removed: Use ICU.

    /**
     * Constructs a <code>Currency</code> instance. The constructor is private
     * so that we can insure that there's never more than one instance for a
     * given currency.
     */
    // BEGIN Android-changed: Use ICU.
    // We do not keep track of defaultFractionDigits and numericCode separately.
    /*
    private Currency(String currencyCode, int defaultFractionDigits, int numericCode) {
        this.currencyCode = currencyCode;
        this.defaultFractionDigits = defaultFractionDigits;
        this.numericCode = numericCode;
    }
    */
    // private Currency(android.icu.util.Currency icuCurrency) {
    //     this.icuCurrency = icuCurrency;
    //     this.currencyCode = icuCurrency.getCurrencyCode();
    // }
    // END Android-changed: Use ICU.
    private Currency(String currencyCode) {
        this.currencyCode = currencyCode;
        if (!availableCurrencyCodes.contains(currencyCode)) {
            throw new IllegalArgumentException("Unsupported ISO 4217 currency code: " +
                    currencyCode);
        }
    }

    /**
     * Returns the <code>Currency</code> instance for the given currency code.
     *
     * @param currencyCode the ISO 4217 code of the currency
     * @return the <code>Currency</code> instance for the given currency code
     * @exception NullPointerException if <code>currencyCode</code> is null
     * @exception IllegalArgumentException if <code>currencyCode</code> is not
     * a supported ISO 4217 code.
     */
    // public static Currency getInstance(String currencyCode) {
    //     // BEGIN Android-changed: Use ICU.
    //     // Upstream uses a private static helper method, implemented differently.
    //     Currency instance = instances.get(currencyCode);
    //     if (instance != null) {
    //         return instance;
    //     }
    //     android.icu.util.Currency icuInstance =
    //               android.icu.util.Currency.getInstance(currencyCode);
    //     if (icuInstance == null) {
    //         return null;
    //     }
    //     /*
    //     if (defaultFractionDigits == Integer.MIN_VALUE) {
    //         // Currency code not internally generated, need to verify first
    //         // A currency code must have 3 characters and exist in the main table
    //         // or in the list of other currencies.
    //         boolean found = false;
    //         if (currencyCode.length() != 3) {
    //             throw new IllegalArgumentException();
    //         }
    //         char char1 = currencyCode.charAt(0);
    //         char char2 = currencyCode.charAt(1);
    //         int tableEntry = getMainTableEntry(char1, char2);
    //         if ((tableEntry & COUNTRY_TYPE_MASK) == SIMPLE_CASE_COUNTRY_MASK
    //                 && tableEntry != INVALID_COUNTRY_ENTRY
    //                 && currencyCode.charAt(2) - 'A' == (tableEntry & SIMPLE_CASE_COUNTRY_FINAL_CHAR_MASK)) {
    //             defaultFractionDigits = (tableEntry & SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_MASK) >> SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT;
    //             numericCode = (tableEntry & NUMERIC_CODE_MASK) >> NUMERIC_CODE_SHIFT;
    //             found = true;
    //         } else { //special case
    //             int[] fractionAndNumericCode = SpecialCaseEntry.findEntry(currencyCode);
    //             if (fractionAndNumericCode != null) {
    //                 defaultFractionDigits = fractionAndNumericCode[0];
    //                 numericCode = fractionAndNumericCode[1];
    //                 found = true;
    //             }
    //         }

    //         if (!found) {
    //             OtherCurrencyEntry ocEntry = OtherCurrencyEntry.findEntry(currencyCode);
    //             if (ocEntry == null) {
    //                 throw new IllegalArgumentException();
    //             }
    //             defaultFractionDigits = ocEntry.fraction;
    //             numericCode = ocEntry.numericCode;
    //         }
    //     }
    //     */

    //     Currency currencyVal = new Currency(icuInstance);
    //     // END Android-changed: Use ICU.
    //     instance = instances.putIfAbsent(currencyCode, currencyVal);
    //     return (instance != null ? instance : currencyVal);
    // }
    public static Currency getInstance(String currencyCode) {
        synchronized (codesToCurrencies) {
            Currency currency = codesToCurrencies.get(currencyCode);
            if (currency == null) {
                currency = new Currency(currencyCode);
                codesToCurrencies.put(currencyCode, currency);
            }
            return currency;
        }
    }

    // Android-changed: Remove "rg" support in the javadoc. See http://b/228322300.
    /**
     * Returns the <code>Currency</code> instance for the country of the
     * given locale. The language and variant components of the locale
     * are ignored. The result may vary over time, as countries change their
     * currencies. For example, for the original member countries of the
     * European Monetary Union, the method returns the old national currencies
     * until December 31, 2001, and the Euro from January 1, 2002, local time
     * of the respective countries.
     * <p>
     * If the specified {@code locale} contains "cu"
     * <a href="./Locale.html#def_locale_extension">Unicode extensions</a>,
     * the instance returned from this method reflects
     * the values specified with those extensions.
     * <p>
     * The method returns <code>null</code> for territories that don't
     * have a currency, such as Antarctica.
     *
     * @param locale the locale for whose country a <code>Currency</code>
     * instance is needed
     * @return the <code>Currency</code> instance for the country of the given
     * locale, or {@code null}
     * @exception NullPointerException if <code>locale</code>
     * is {@code null}
     * @exception IllegalArgumentException if the country of the given {@code locale}
     * is not a supported ISO 3166 country code.
     */
    public static Currency getInstance(Locale locale) {
        synchronized (localesToCurrencies) {
            if (locale == null) {
                throw new NullPointerException("locale == null");
            }
            Currency currency = localesToCurrencies.get(locale);
            if (currency != null) {
                return currency;
            }

            String currencyCode = ICU.getCurrencyCode(locale);
            if (currencyCode == null) {
                // Don't cache -- this is rarely necessary since most countries have currencies.
                if (Arrays.asList(Locale.getISOCountries()).contains(locale.getCountry())) {
                    return null;
                }
                throw new IllegalArgumentException("Unsupported ISO 3166 country: " + locale);
            } else if (currencyCode.equals("XXX")) {
                return null;
            }
            Currency result = getInstance(currencyCode);
            localesToCurrencies.put(locale, result);
            return result;
        }
    }

    private static LinkedHashSet<String> constructAvailableCurrencyCodes() {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (String code : ICU.getAvailableCurrencyCodes()) {
          result.add(code);
        }
        return result;
    }

    /**
     * Gets the set of available currencies.  The returned set of currencies
     * contains all of the available currencies, which may include currencies
     * that represent obsolete ISO 4217 codes.  The set can be modified
     * without affecting the available currencies in the runtime.
     *
     * @return the set of available currencies.  If there is no currency
     *    available in the runtime, the returned set is empty.
     * @since 1.7
     */
    public static Set<Currency> getAvailableCurrencies() {
        Set<Currency> result = new LinkedHashSet<Currency>();
        for (String currencyCode : availableCurrencyCodes) {
            result.add(Currency.getInstance(currencyCode));
        }
        return result;
    }

    /**
     * Gets the ISO 4217 currency code of this currency.
     *
     * @return the ISO 4217 currency code of this currency.
     */
    public String getCurrencyCode() {
        return currencyCode;
    }

    // Android-changed: Remove "rg" support in the javadoc. See http://b/228322300.
    /**
     * Gets the symbol of this currency for the default
     * {@link Locale.Category#DISPLAY DISPLAY} locale.
     * For example, for the US Dollar, the symbol is "$" if the default
     * locale is the US, while for other locales it may be "US$". If no
     * symbol can be determined, the ISO 4217 currency code is returned.
     * <p>
     * This is equivalent to calling
     * {@link #getSymbol(Locale)
     *     getSymbol(Locale.getDefault(Locale.Category.DISPLAY))}.
     *
     * @return the symbol of this currency for the default
     *     {@link Locale.Category#DISPLAY DISPLAY} locale
     */
    public String getSymbol() {
        return getSymbol(Locale.getDefault(Locale.Category.DISPLAY));
    }

    // Android-changed: Remove "rg" support in the javadoc. See http://b/228322300.
    /**
     * Gets the symbol of this currency for the specified locale.
     * For example, for the US Dollar, the symbol is "$" if the specified
     * locale is the US, while for other locales it may be "US$". If no
     * symbol can be determined, the ISO 4217 currency code is returned.
     *
     * @param locale the locale for which a display name for this currency is
     * needed
     * @return the symbol of this currency for the specified locale
     * @exception NullPointerException if <code>locale</code> is null
     */
    public String getSymbol(Locale locale) {
        // BEGIN Android-changed: Use ICU.
        /*
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(CurrencyNameProvider.class);
        locale = CalendarDataUtility.findRegionOverride(locale);
        String symbol = pool.getLocalizedObject(
                                CurrencyNameGetter.INSTANCE,
                                locale, currencyCode, SYMBOL);
        if (symbol != null) {
            return symbol;
        }

        // use currency code as symbol of last resort
        return currencyCode;
        */
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        // Check the locale first, in case the locale has the same currency.
        LocaleData localeData = LocaleData.get(locale);
        if (localeData.internationalCurrencySymbol.equals(currencyCode)) {
            return localeData.currencySymbol;
        }

        // Try ICU, and fall back to the currency code if ICU has nothing.
        String symbol = ICU.getCurrencySymbol(locale, currencyCode);
        return symbol != null ? symbol : currencyCode;
    }

    /**
     * Gets the default number of fraction digits used with this currency.
     * Note that the number of fraction digits is the same as ISO 4217's
     * minor unit for the currency.
     * For example, the default number of fraction digits for the Euro is 2,
     * while for the Japanese Yen it's 0.
     * In the case of pseudo-currencies, such as IMF Special Drawing Rights,
     * -1 is returned.
     *
     * @return the default number of fraction digits used with this currency
    */
    public int getDefaultFractionDigits() {
        // In some places the code XXX is used as the fall back currency.
        // The RI returns -1, but ICU defaults to 2 for unknown currencies.
        if (currencyCode.equals("XXX")) {
            return -1;
        }
        return ICU.getCurrencyFractionDigits(currencyCode);
    }

    /**
     * Returns the ISO 4217 numeric code of this currency.
     *
     * @return the ISO 4217 numeric code of this currency
     * @since 1.7
     */
    public int getNumericCode() {
        try {
            String name = "com.google.j2objc.util.CurrencyNumericCodesImpl";
            CurrencyNumericCodes cnc = (CurrencyNumericCodes) Class.forName(name).newInstance();
            return cnc.getNumericCode(currencyCode);
        } catch (Exception e) {
            throw new LibraryNotLinkedError("java.util support", "jre_util",
                "ComGoogleJ2objcUtilCurrencyNumericCodesImpl");
        }
    }

    /**
     * Returns the 3 digit ISO 4217 numeric code of this currency as a {@code String}.
     * Unlike {@link #getNumericCode()}, which returns the numeric code as {@code int},
     * this method always returns the numeric code as a 3 digit string.
     * e.g. a numeric value of 32 would be returned as "032",
     * and a numeric value of 6 would be returned as "006".
     *
     * @return the 3 digit ISO 4217 numeric code of this currency as a {@code String}
     * @since 9
     */
    public String getNumericCodeAsString() {
        // Android-added: We don't store the code as a field. Call getNumericCode().
        int numericCode = getNumericCode();
        /* numeric code could be returned as a 3 digit string simply by using
           String.format("%03d",numericCode); which uses regex to parse the format,
           "%03d" in this case. Parsing a regex gives an extra performance overhead,
           so String.format() approach is avoided in this scenario.
        */
        if (numericCode < 100) {
            StringBuilder sb = new StringBuilder();
            sb.append('0');
            if (numericCode < 10) {
                sb.append('0');
            }
            return sb.append(numericCode).toString();
        }
        return String.valueOf(numericCode);
    }

    /**
     * Gets the name that is suitable for displaying this currency for
     * the default {@link Locale.Category#DISPLAY DISPLAY} locale.
     * If there is no suitable display name found
     * for the default locale, the ISO 4217 currency code is returned.
     * <p>
     * This is equivalent to calling
     * {@link #getDisplayName(Locale)
     *     getDisplayName(Locale.getDefault(Locale.Category.DISPLAY))}.
     *
     * @return the display name of this currency for the default
     *     {@link Locale.Category#DISPLAY DISPLAY} locale
     * @since 1.7
     */
    public String getDisplayName() {
        return getDisplayName(Locale.getDefault(Locale.Category.DISPLAY));
    }

    /**
     * Gets the name that is suitable for displaying this currency for
     * the specified locale.  If there is no suitable display name found
     * for the specified locale, the ISO 4217 currency code is returned.
     *
     * @param locale the locale for which a display name for this currency is
     * needed
     * @return the display name of this currency for the specified locale
     * @exception NullPointerException if <code>locale</code> is null
     * @since 1.7
     */
    public String getDisplayName(Locale locale) {
        return ICU.getCurrencyDisplayName(locale, currencyCode);
    }

    /**
     * Returns the ISO 4217 currency code of this currency.
     *
     * @return the ISO 4217 currency code of this currency
     */
    @Override
    public String toString() {
        return currencyCode;
    }

    /**
     * Resolves instances being deserialized to a single instance per currency.
     */
    private Object readResolve() {
        return getInstance(currencyCode);
    }

    // Android-removed: Use ICU.
    // Removed a bunch of private helper methods that are unused on Android.
    /**
     * Gets the main table entry for the country whose country code consists
     * of char1 and char2.
     *
    private static int getMainTableEntry(char char1, char char2) {
        if (char1 < 'A' || char1 > 'Z' || char2 < 'A' || char2 > 'Z') {
            throw new IllegalArgumentException();
        }
        return mainTable[(char1 - 'A') * A_TO_Z + (char2 - 'A')];
    }

    /**
     * Sets the main table entry for the country whose country code consists
     * of char1 and char2.
     *
    private static void setMainTableEntry(char char1, char char2, int entry) {
        if (char1 < 'A' || char1 > 'Z' || char2 < 'A' || char2 > 'Z') {
            throw new IllegalArgumentException();
        }
        mainTable[(char1 - 'A') * A_TO_Z + (char2 - 'A')] = entry;
    }

    /**
     * Obtains a localized currency names from a CurrencyNameProvider
     * implementation.
     *
    private static class CurrencyNameGetter
        implements LocaleServiceProviderPool.LocalizedObjectGetter<CurrencyNameProvider,
                                                                   String> {
        private static final CurrencyNameGetter INSTANCE = new CurrencyNameGetter();

        @Override
        public String getObject(CurrencyNameProvider currencyNameProvider,
                                Locale locale,
                                String key,
                                Object... params) {
            assert params.length == 1;
            int type = (Integer)params[0];

            switch(type) {
            case SYMBOL:
                return currencyNameProvider.getSymbol(key, locale);
            case DISPLAYNAME:
                return currencyNameProvider.getDisplayName(key, locale);
            default:
                assert false; // shouldn't happen
            }

            return null;
        }
    }

    private static int[] readIntArray(DataInputStream dis, int count) throws IOException {
        int[] ret = new int[count];
        for (int i = 0; i < count; i++) {
            ret[i] = dis.readInt();
        }

        return ret;
    }

    private static List<SpecialCaseEntry> readSpecialCases(DataInputStream dis,
            int count)
            throws IOException {

        List<SpecialCaseEntry> list = new ArrayList<>(count);
        long cutOverTime;
        String oldCurrency;
        String newCurrency;
        int oldCurrencyFraction;
        int newCurrencyFraction;
        int oldCurrencyNumericCode;
        int newCurrencyNumericCode;

        for (int i = 0; i < count; i++) {
            cutOverTime = dis.readLong();
            oldCurrency = dis.readUTF();
            newCurrency = dis.readUTF();
            oldCurrencyFraction = dis.readInt();
            newCurrencyFraction = dis.readInt();
            oldCurrencyNumericCode = dis.readInt();
            newCurrencyNumericCode = dis.readInt();
            SpecialCaseEntry sc = new SpecialCaseEntry(cutOverTime,
                    oldCurrency, newCurrency,
                    oldCurrencyFraction, newCurrencyFraction,
                    oldCurrencyNumericCode, newCurrencyNumericCode);
            list.add(sc);
        }
        return list;
    }

    private static List<OtherCurrencyEntry> readOtherCurrencies(DataInputStream dis,
            int count)
            throws IOException {

        List<OtherCurrencyEntry> list = new ArrayList<>(count);
        String currencyCode;
        int fraction;
        int numericCode;

        for (int i = 0; i < count; i++) {
            currencyCode = dis.readUTF();
            fraction = dis.readInt();
            numericCode = dis.readInt();
            OtherCurrencyEntry oc = new OtherCurrencyEntry(currencyCode,
                    fraction,
                    numericCode);
            list.add(oc);
        }
        return list;
    }

    /**
     * Parse currency data found in the properties file (that
     * java.util.currency.data designates) to a List of CurrencyProperty
     * instances. Also, remove invalid entries and the multiple currency
     * code inconsistencies.
     *
     * @param props properties containing currency data
     * @param pattern regex pattern for the properties entry
     * @return list of parsed property entries
     *
    private static List<CurrencyProperty> getValidCurrencyData(Properties props,
            Pattern pattern) {

        Set<String> keys = props.stringPropertyNames();
        List<CurrencyProperty> propertyEntries = new ArrayList<>();

        // remove all invalid entries and parse all valid currency properties
        // entries to a group of CurrencyProperty, classified by currency code
        Map<String, List<CurrencyProperty>> currencyCodeGroup = keys.stream()
                .map(k -> CurrencyProperty
                .getValidEntry(k.toUpperCase(Locale.ROOT),
                        props.getProperty(k).toUpperCase(Locale.ROOT),
                        pattern)).flatMap(o -> o.stream())
                .collect(Collectors.groupingBy(entry -> entry.currencyCode));

        // check each group for inconsistencies
        currencyCodeGroup.forEach((curCode, list) -> {
            boolean inconsistent = CurrencyProperty
                    .containsInconsistentInstances(list);
            if (inconsistent) {
                list.forEach(prop -> CurrencyProperty.info("The property"
                        + " entry for " + prop.country + " is inconsistent."
                        + " Ignored.", null));
            } else {
                propertyEntries.addAll(list);
            }
        });

        return propertyEntries;
    }

    /**
     * Replaces currency data found in the properties file that
     * java.util.currency.data designates. This method is invoked for
     * each valid currency entry.
     *
     * @param prop CurrencyProperty instance of the valid property entry
     *
    private static void replaceCurrencyData(CurrencyProperty prop) {


        String ctry = prop.country;
        String code = prop.currencyCode;
        int numeric = prop.numericCode;
        int fraction = prop.fraction;
        int entry = numeric << NUMERIC_CODE_SHIFT;

        int index = SpecialCaseEntry.indexOf(code, fraction, numeric);


        // If a new entry changes the numeric code/dfd of an existing
        // currency code, update it in the sc list at the respective
        // index and also change it in the other currencies list and
        // main table (if that currency code is also used as a
        // simple case).

        // If all three components do not match with the new entry,
        // but the currency code exists in the special case list
        // update the sc entry with the new entry
        int scCurrencyCodeIndex = -1;
        if (index == -1) {
            scCurrencyCodeIndex = SpecialCaseEntry.currencyCodeIndex(code);
            if (scCurrencyCodeIndex != -1) {
                //currency code exists in sc list, then update the old entry
                specialCasesList.set(scCurrencyCodeIndex,
                        new SpecialCaseEntry(code, fraction, numeric));

                // also update the entry in other currencies list
                OtherCurrencyEntry oe = OtherCurrencyEntry.findEntry(code);
                if (oe != null) {
                    int oIndex = otherCurrenciesList.indexOf(oe);
                    otherCurrenciesList.set(oIndex, new OtherCurrencyEntry(
                            code, fraction, numeric));
                }
            }
        }

        /* If a country switches from simple case to special case or
         * one special case to other special case which is not present
         * in the sc arrays then insert the new entry in special case arrays.
         * If an entry with given currency code exists, update with the new
         * entry.
         *
        if (index == -1 && (ctry.charAt(0) != code.charAt(0)
                || ctry.charAt(1) != code.charAt(1))) {

            if(scCurrencyCodeIndex == -1) {
                specialCasesList.add(new SpecialCaseEntry(code, fraction,
                        numeric));
                index = specialCasesList.size() - 1;
            } else {
                index = scCurrencyCodeIndex;
            }

            // update the entry in main table if it exists as a simple case
            updateMainTableEntry(code, fraction, numeric);
        }

        if (index == -1) {
            // simple case
            entry |= (fraction << SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT)
                    | (code.charAt(2) - 'A');
        } else {
            // special case
            entry = SPECIAL_CASE_COUNTRY_MASK
                    | (index + SPECIAL_CASE_COUNTRY_INDEX_DELTA);
        }
        setMainTableEntry(ctry.charAt(0), ctry.charAt(1), entry);
    }

    // update the entry in maintable for any simple case found, if a new
    // entry as a special case updates the entry in sc list with
    // existing currency code
    private static void updateMainTableEntry(String code, int fraction,
            int numeric) {
        // checking the existence of currency code in mainTable
        int tableEntry = getMainTableEntry(code.charAt(0), code.charAt(1));
        int entry = numeric << NUMERIC_CODE_SHIFT;
        if ((tableEntry & COUNTRY_TYPE_MASK) == SIMPLE_CASE_COUNTRY_MASK
                && tableEntry != INVALID_COUNTRY_ENTRY
                && code.charAt(2) - 'A' == (tableEntry
                & SIMPLE_CASE_COUNTRY_FINAL_CHAR_MASK)) {

            int numericCode = (tableEntry & NUMERIC_CODE_MASK)
                    >> NUMERIC_CODE_SHIFT;
            int defaultFractionDigits = (tableEntry
                    & SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_MASK)
                    >> SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT;
            if (numeric != numericCode || fraction != defaultFractionDigits) {
                // update the entry in main table
                entry |= (fraction << SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT)
                        | (code.charAt(2) - 'A');
                setMainTableEntry(code.charAt(0), code.charAt(1), entry);
            }
        }
    }

    /* Used to represent a special case currency entry
     * - cutOverTime: cut-over time in millis as returned by
     *   System.currentTimeMillis for special case countries that are changing
     *   currencies; Long.MAX_VALUE for countries that are not changing currencies
     * - oldCurrency: old currencies for special case countries
     * - newCurrency: new currencies for special case countries that are
     *   changing currencies; null for others
     * - oldCurrencyFraction: default fraction digits for old currencies
     * - newCurrencyFraction: default fraction digits for new currencies, 0 for
     *   countries that are not changing currencies
     * - oldCurrencyNumericCode: numeric code for old currencies
     * - newCurrencyNumericCode: numeric code for new currencies, 0 for countries
     *   that are not changing currencies
    *
    private static class SpecialCaseEntry {

        final private long cutOverTime;
        final private String oldCurrency;
        final private String newCurrency;
        final private int oldCurrencyFraction;
        final private int newCurrencyFraction;
        final private int oldCurrencyNumericCode;
        final private int newCurrencyNumericCode;

        private SpecialCaseEntry(long cutOverTime, String oldCurrency, String newCurrency,
                int oldCurrencyFraction, int newCurrencyFraction,
                int oldCurrencyNumericCode, int newCurrencyNumericCode) {
            this.cutOverTime = cutOverTime;
            this.oldCurrency = oldCurrency;
            this.newCurrency = newCurrency;
            this.oldCurrencyFraction = oldCurrencyFraction;
            this.newCurrencyFraction = newCurrencyFraction;
            this.oldCurrencyNumericCode = oldCurrencyNumericCode;
            this.newCurrencyNumericCode = newCurrencyNumericCode;
        }

        private SpecialCaseEntry(String currencyCode, int fraction,
                int numericCode) {
            this(Long.MAX_VALUE, currencyCode, "", fraction, 0, numericCode, 0);
        }

        //get the index of the special case entry
        private static int indexOf(String code, int fraction, int numeric) {
            int size = specialCasesList.size();
            for (int index = 0; index < size; index++) {
                SpecialCaseEntry scEntry = specialCasesList.get(index);
                if (scEntry.oldCurrency.equals(code)
                        && scEntry.oldCurrencyFraction == fraction
                        && scEntry.oldCurrencyNumericCode == numeric
                        && scEntry.cutOverTime == Long.MAX_VALUE) {
                    return index;
                }
            }
            return -1;
        }

        // get the fraction and numericCode of the sc currencycode
        private static int[] findEntry(String code) {
            int[] fractionAndNumericCode = null;
            int size = specialCasesList.size();
            for (int index = 0; index < size; index++) {
                SpecialCaseEntry scEntry = specialCasesList.get(index);
                if (scEntry.oldCurrency.equals(code) && (scEntry.cutOverTime == Long.MAX_VALUE
                        || System.currentTimeMillis() < scEntry.cutOverTime)) {
                    //consider only when there is no new currency or cutover time is not passed
                    fractionAndNumericCode = new int[2];
                    fractionAndNumericCode[0] = scEntry.oldCurrencyFraction;
                    fractionAndNumericCode[1] = scEntry.oldCurrencyNumericCode;
                    break;
                } else if (scEntry.newCurrency.equals(code)
                        && System.currentTimeMillis() >= scEntry.cutOverTime) {
                    //consider only if the cutover time is passed
                    fractionAndNumericCode = new int[2];
                    fractionAndNumericCode[0] = scEntry.newCurrencyFraction;
                    fractionAndNumericCode[1] = scEntry.newCurrencyNumericCode;
                    break;
                }
            }
            return fractionAndNumericCode;
        }

        // get the index based on currency code
        private static int currencyCodeIndex(String code) {
            int size = specialCasesList.size();
            for (int index = 0; index < size; index++) {
                SpecialCaseEntry scEntry = specialCasesList.get(index);
                if (scEntry.oldCurrency.equals(code) && (scEntry.cutOverTime == Long.MAX_VALUE
                        || System.currentTimeMillis() < scEntry.cutOverTime)) {
                    //consider only when there is no new currency or cutover time is not passed
                    return index;
                } else if (scEntry.newCurrency.equals(code)
                        && System.currentTimeMillis() >= scEntry.cutOverTime) {
                    //consider only if the cutover time is passed
                    return index;
                }
            }
            return -1;
        }


        // convert the special case entry to sc arrays index
        private static int toIndex(int tableEntry) {
            return (tableEntry & SPECIAL_CASE_COUNTRY_INDEX_MASK) - SPECIAL_CASE_COUNTRY_INDEX_DELTA;
        }

    }

    /* Used to represent Other currencies
     * - currencyCode: currency codes that are not the main currency
     *   of a simple country
     * - otherCurrenciesDFD: decimal format digits for other currencies
     * - otherCurrenciesNumericCode: numeric code for other currencies
     *
    private static class OtherCurrencyEntry {

        final private String currencyCode;
        final private int fraction;
        final private int numericCode;

        private OtherCurrencyEntry(String currencyCode, int fraction,
                int numericCode) {
            this.currencyCode = currencyCode;
            this.fraction = fraction;
            this.numericCode = numericCode;
        }

        //get the instance of the other currency code
        private static OtherCurrencyEntry findEntry(String code) {
            int size = otherCurrenciesList.size();
            for (int index = 0; index < size; index++) {
                OtherCurrencyEntry ocEntry = otherCurrenciesList.get(index);
                if (ocEntry.currencyCode.equalsIgnoreCase(code)) {
                    return ocEntry;
                }
            }
            return null;
        }

    }


    /*
     * Used to represent an entry of the properties file that
     * java.util.currency.data designates
     *
     * - country: country representing the currency entry
     * - currencyCode: currency code
     * - fraction: default fraction digit
     * - numericCode: numeric code
     * - date: cutover date
     *
    private static class CurrencyProperty {
        final private String country;
        final private String currencyCode;
        final private int fraction;
        final private int numericCode;
        final private String date;

        private CurrencyProperty(String country, String currencyCode,
                int fraction, int numericCode, String date) {
            this.country = country;
            this.currencyCode = currencyCode;
            this.fraction = fraction;
            this.numericCode = numericCode;
            this.date = date;
        }

        /**
         * Check the valid currency data and create/return an Optional instance
         * of CurrencyProperty
         *
         * @param ctry    country representing the currency data
         * @param curData currency data of the given {@code ctry}
         * @param pattern regex pattern for the properties entry
         * @return Optional containing CurrencyProperty instance, If valid;
         *         empty otherwise
         *
        private static Optional<CurrencyProperty> getValidEntry(String ctry,
                String curData,
                Pattern pattern) {

            CurrencyProperty prop = null;

            if (ctry.length() != 2) {
                // Invalid country code. Ignore the entry.
            } else {

                prop = parseProperty(ctry, curData, pattern);
                // if the property entry failed any of the below checked
                // criteria it is ignored
                if (prop == null
                        || (prop.date == null && curData.chars()
                                .map(c -> c == ',' ? 1 : 0).sum() >= 3)) {
                    // format is not recognized.  ignore the data if date
                    // string is null and we've 4 values, bad date value
                    prop = null;
                } else if (prop.fraction
                        > SIMPLE_CASE_COUNTRY_MAX_DEFAULT_DIGITS) {
                    prop = null;
                } else {
                    try {
                        if (prop.date != null
                                && !isPastCutoverDate(prop.date)) {
                            prop = null;
                        }
                    } catch (ParseException ex) {
                        prop = null;
                    }
                }
            }

            if (prop == null) {
                info("The property entry for " + ctry + " is invalid."
                        + " Ignored.", null);
            }

            return Optional.ofNullable(prop);
        }

        /*
         * Parse properties entry and return CurrencyProperty instance
         *
        private static CurrencyProperty parseProperty(String ctry,
                String curData, Pattern pattern) {
            Matcher m = pattern.matcher(curData);
            if (!m.find()) {
                return null;
            } else {
                return new CurrencyProperty(ctry, m.group(1),
                        Integer.parseInt(m.group(3)),
                        Integer.parseInt(m.group(2)), m.group(4));
            }
        }

        /**
         * Checks if the given list contains multiple inconsistent currency instances
         *
        private static boolean containsInconsistentInstances(
                List<CurrencyProperty> list) {
            int numCode = list.get(0).numericCode;
            int fractionDigit = list.get(0).fraction;
            return list.stream().anyMatch(prop -> prop.numericCode != numCode
                    || prop.fraction != fractionDigit);
        }

        private static boolean isPastCutoverDate(String s)
                throws ParseException {
            SimpleDateFormat format = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            format.setLenient(false);
            long time = format.parse(s.trim()).getTime();
            return System.currentTimeMillis() > time;

        }

        private static void info(String message, Throwable t) {
            PlatformLogger logger = PlatformLogger
                    .getLogger("java.util.Currency");
            if (logger.isLoggable(PlatformLogger.Level.INFO)) {
                if (t != null) {
                    logger.info(message, t);
                } else {
                    logger.info(message);
                }
            }
        }

    }
    */
}


