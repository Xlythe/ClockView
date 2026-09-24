package com.xlythe.watchface.screenshots.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.GoalProgressComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.RangedValueComplicationData;
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed complication data for the screenshot faces, one data source per band. Each is named in
 * AndroidManifest.xml, which is also where its type is declared, and bound to its band by the
 * face's primaryProvider.
 */
public abstract class Sources extends ComplicationDataSourceService {
    /** Wear's own palette, so the elements are told apart the way a real provider's would be. */
    private static final int[] COLORS = {0xFF4285F4, 0xFF34A853, 0xFFFBBC05, 0xFFEA4335, 0xFFA142F4};

    abstract ComplicationData data();

    @Override
    public void onComplicationRequest(@NonNull ComplicationRequest request, @NonNull ComplicationRequestListener listener) {
        try {
            listener.onComplicationData(data());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    public ComplicationData getPreviewData(@NonNull ComplicationType type) {
        return data();
    }

    private static PlainComplicationText text(String text) {
        return new PlainComplicationText.Builder(text).build();
    }

    static ComplicationData ranged(float percent) {
        return new RangedValueComplicationData.Builder(percent, 0f, 100f, text(percent + "%"))
                .setText(text((int) percent + "%"))
                .build();
    }

    static ComplicationData goal(float percent) {
        return new GoalProgressComplicationData.Builder(percent * 100, 10000f, text(percent + "% of goal"))
                .setText(text((int) (percent * 100) + ""))
                .build();
    }

    /** Weights falling by one each time - 3, 2, 1 - so no two elements are the same length. */
    static ComplicationData weighted(int count) {
        List<WeightedElementsComplicationData.Element> elements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            elements.add(new WeightedElementsComplicationData.Element(count - i, COLORS[i % COLORS.length]));
        }
        return new WeightedElementsComplicationData.Builder(elements, text(count + " elements"))
                .setText(text(count + ""))
                .build();
    }

    public static class Ranged0 extends Sources { ComplicationData data() { return ranged(0); } }
    public static class Ranged15 extends Sources { ComplicationData data() { return ranged(15); } }
    public static class Ranged60 extends Sources { ComplicationData data() { return ranged(60); } }
    public static class Ranged100 extends Sources { ComplicationData data() { return ranged(100); } }

    public static class Goal0 extends Sources { ComplicationData data() { return goal(0); } }
    public static class Goal70 extends Sources { ComplicationData data() { return goal(70); } }
    public static class Goal100 extends Sources { ComplicationData data() { return goal(100); } }
    public static class Goal130 extends Sources { ComplicationData data() { return goal(130); } }

    public static class Weighted1 extends Sources { ComplicationData data() { return weighted(1); } }
    public static class Weighted2 extends Sources { ComplicationData data() { return weighted(2); } }
    public static class Weighted3 extends Sources { ComplicationData data() { return weighted(3); } }
    public static class Weighted5 extends Sources { ComplicationData data() { return weighted(5); } }
}
