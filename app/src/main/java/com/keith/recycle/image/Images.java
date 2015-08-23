package com.keith.recycle.image;

import java.util.Random;

/**
 * Created by keith on 15/8/23.
 */
public class Images {

    private static final int[] VALUES = {360, 475, 420, 500, 320, 540, 600, 390, 450, 560};
    private static final int IMAGE_AMOUNT = 100;

    public static Pair[] SIZES;
    public static String[] URIS;

    public static void init() {
        SIZES = new Pair[IMAGE_AMOUNT];
        URIS = new String[IMAGE_AMOUNT];
        Random random = new Random();
        for (int i = 0; i < IMAGE_AMOUNT; i++) {
            int wIndex = random.nextInt(VALUES.length);
            int hIndex = random.nextInt(VALUES.length);

            SIZES[i] = new Pair(VALUES[wIndex], VALUES[hIndex]);

            String uri = "https://placeholdit.imgix.net/~text?txtsize=32&txt=image-" + i + "&w=" + VALUES[wIndex] + "&h=" + VALUES[hIndex];
            URIS[i] = uri;
        }
    }

}
