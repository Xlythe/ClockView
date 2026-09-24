package com.xlythe.watchface.screenshots.provider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.Icon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.watchface.complications.data.ColorRamp;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.GoalProgressComplicationData;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.MonochromaticImage;
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData;
import androidx.wear.watchface.complications.data.PhotoImageComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.RangedValueComplicationData;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.data.SmallImage;
import androidx.wear.watchface.complications.data.SmallImageComplicationData;
import androidx.wear.watchface.complications.data.SmallImageType;
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Serves one case from cases.groovy. The build generates a subclass per case, named for it, and
 * the case's data is looked up in assets/cases.json by that name.
 */
public abstract class Sources extends ComplicationDataSourceService {
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
        try {
            return data();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ComplicationData data() throws Exception {
        JSONObject c = load(this).getJSONObject(getClass().getSimpleName());
        ComplicationText description = text(c.optString("caption", "screenshot"));
        switch (c.getString("type")) {
            case "SHORT_TEXT": {
                ShortTextComplicationData.Builder builder = new ShortTextComplicationData.Builder(text(c.getString("text")), description);
                if (c.has("title")) builder.setTitle(text(c.getString("title")));
                if (c.optBoolean("icon")) builder.setMonochromaticImage(monochromaticImage());
                if (c.has("smallImage")) builder.setSmallImage(smallImage(c.getString("smallImage")));
                return builder.build();
            }
            case "LONG_TEXT": {
                LongTextComplicationData.Builder builder = new LongTextComplicationData.Builder(text(c.getString("text")), description);
                if (c.has("title")) builder.setTitle(text(c.getString("title")));
                if (c.optBoolean("icon")) builder.setMonochromaticImage(monochromaticImage());
                if (c.has("smallImage")) builder.setSmallImage(smallImage(c.getString("smallImage")));
                return builder.build();
            }
            case "RANGED_VALUE": {
                RangedValueComplicationData.Builder builder = new RangedValueComplicationData.Builder(
                        (float) c.getDouble("value"), (float) c.getDouble("min"), (float) c.getDouble("max"), description);
                if (c.has("text")) builder.setText(text(c.getString("text")));
                if (c.has("title")) builder.setTitle(text(c.getString("title")));
                if (c.optBoolean("icon")) builder.setMonochromaticImage(monochromaticImage());
                if (c.has("colors")) builder.setColorRamp(new ColorRamp(colors(c.getJSONArray("colors")), c.optBoolean("interpolate")));
                if (saysNothing(c)) builder.setTitle(text(" "));
                return builder.build();
            }
            case "GOAL_PROGRESS": {
                GoalProgressComplicationData.Builder builder = new GoalProgressComplicationData.Builder(
                        (float) c.getDouble("value"), (float) c.getDouble("target"), description);
                if (c.has("text")) builder.setText(text(c.getString("text")));
                if (c.has("title")) builder.setTitle(text(c.getString("title")));
                if (c.optBoolean("icon")) builder.setMonochromaticImage(monochromaticImage());
                if (c.has("colors")) builder.setColorRamp(new ColorRamp(colors(c.getJSONArray("colors")), c.optBoolean("interpolate")));
                if (saysNothing(c)) builder.setTitle(text(" "));
                return builder.build();
            }
            case "WEIGHTED_ELEMENTS": {
                List<WeightedElementsComplicationData.Element> elements = new ArrayList<>();
                JSONArray pairs = c.getJSONArray("elements");
                for (int i = 0; i < pairs.length(); i++) {
                    JSONArray pair = pairs.getJSONArray(i);
                    elements.add(new WeightedElementsComplicationData.Element((float) pair.getDouble(0), Color.parseColor(pair.getString(1))));
                }
                WeightedElementsComplicationData.Builder builder = new WeightedElementsComplicationData.Builder(elements, description);
                if (c.has("text")) builder.setText(text(c.getString("text")));
                if (c.has("title")) builder.setTitle(text(c.getString("title")));
                if (c.optBoolean("icon")) builder.setMonochromaticImage(monochromaticImage());
                if (saysNothing(c)) builder.setTitle(text(" "));
                return builder.build();
            }
            case "MONOCHROMATIC_IMAGE":
                return new MonochromaticImageComplicationData.Builder(monochromaticImage(), description).build();
            case "SMALL_IMAGE":
                return new SmallImageComplicationData.Builder(smallImage(c.getString("smallImage")), description).build();
            case "PHOTO_IMAGE":
                return new PhotoImageComplicationData.Builder(Icon.createWithBitmap(photo(300)), description).build();
            default:
                throw new IllegalArgumentException("Unknown type " + c.getString("type"));
        }
    }

    /**
     * Whether a case carries no text, title or icon. Jetpack refuses a ranged value, a goal or
     * weighted elements with none of the three, so a real provider always sends one; such a case
     * is sent a blank title, which a layout treats as having no text, as the case means.
     */
    private static boolean saysNothing(JSONObject c) {
        return !c.has("text") && !c.has("title") && !c.optBoolean("icon");
    }

    private static JSONObject sCases;

    private static synchronized JSONObject load(Context context) throws Exception {
        if (sCases == null) {
            try (InputStream in = context.getAssets().open("cases.json")) {
                sCases = new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return sCases;
    }

    private static ComplicationText text(String text) {
        return new PlainComplicationText.Builder(text).build();
    }

    private static int[] colors(JSONArray array) throws JSONException {
        int[] colors = new int[array.length()];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.parseColor(array.getString(i));
        }
        return colors;
    }

    private MonochromaticImage monochromaticImage() {
        return new MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.monochromatic_image)).build();
    }

    private SmallImage smallImage(String style) {
        return "PHOTO".equals(style)
                ? new SmallImage.Builder(Icon.createWithBitmap(photo(200)), SmallImageType.PHOTO).build()
                : new SmallImage.Builder(Icon.createWithResource(this, R.drawable.small_image), SmallImageType.ICON).build();
    }

    /** A landscape at dusk, drawn rather than shipped, and the same every time. */
    private static Bitmap photo(int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0, 0, 0, size, 0xFF1A237E, 0xFFFF8A65, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, size, size, paint);
        paint.setShader(null);
        paint.setColor(0xFFFFE082);
        canvas.drawCircle(size * 0.68f, size * 0.52f, size * 0.12f, paint);
        Path hills = new Path();
        hills.moveTo(0, size * 0.72f);
        hills.quadTo(size * 0.3f, size * 0.55f, size * 0.55f, size * 0.7f);
        hills.quadTo(size * 0.8f, size * 0.82f, size, size * 0.66f);
        hills.lineTo(size, size);
        hills.lineTo(0, size);
        hills.close();
        paint.setColor(0xFF2E7D32);
        canvas.drawPath(hills, paint);
        return bitmap;
    }
}
