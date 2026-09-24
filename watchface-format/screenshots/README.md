# Complication screenshots

Pictures of every complication layout the plugin bundles - chips, arcs and backgrounds, each
complication type they take, the arrangements each type falls into depending on what the provider
sends, colour ramps and the four drawable styles - as a watch draws them, lit and in ambient. They
are kept as goldens so a change to the plugin shows up as a change to a picture.

**[See the goldens](goldens/README.md).**

Watch Face Format is drawn by the watch rather than by anything on the JVM, so these are taken on
one. This directory is its own Gradle build. Every case is a line in
[`cases.groovy`](cases.groovy); from that table it builds a watch face per entry with the plugin
in this checkout, a data source per case in [`provider/`](provider), photographs each face lit and
in ambient, and compares the photographs with [`goldens/`](goldens).

## Running

With a Wear OS device or emulator attached:

```
gradle -p watchface-format/screenshots verifyScreenshots   # fails if anything looks different
gradle -p watchface-format/screenshots recordScreenshots   # replaces the goldens with what it sees
```

`verifyScreenshots` leaves what the device showed in `build/screenshots/actual` and the pixels
that moved, in red over the golden, in `build/screenshots/diff`. When a change is meant to look
different, record, look at the new goldens, and commit them with the change.

The goldens were taken on the `wearos_large_round` emulator (454x454) running
`system-images;android-36;android-wear-signed;x86_64` (Wear OS 6), started with
`-gpu swiftshader_indirect`. Another device draws different pixels, so record on it first or use
the same one. Both builds need an SDK: `sdk.dir` in `local.properties` here and at the root of
the repository, or `ANDROID_HOME`.

Options, as `-P` properties:

- `screenshotDevice` - the serial to use when more than one device is attached (or `ANDROID_SERIAL`).
- `screenshotSettleMillis` - how long a face is given to load its data before it is photographed,
  10000 by default.
- `screenshotTolerance` - the share of pixels allowed to differ, 0.0001 by default.

## Adding a case

Add a line to [`cases.groovy`](cases.groovy) - a face holds four chips, four bands or one
background - and run `recordScreenshots`. The table says what each case is fed; the build writes
the face, the data source and its manifest entry, and the index in `goldens/README.md`.

Some arrangements cannot be reached: Jetpack will not build a `SHORT_TEXT` or `LONG_TEXT` without
text, nor a ranged value, goal or weighted elements with no text, title or icon. A case that asks
for none of the three is sent a blank title, which the layouts treat as no text.
