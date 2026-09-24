// Every screenshot, as data. Each entry is one watch face, photographed lit and in ambient; each
// of its cases is one slot and the complication fed to it. settings.gradle makes a project of
// each face, build.gradle writes its watchface.xml, and the provider is generated from the same
// cases, so adding a line here is all a new case takes.
//
// A face's layout decides where its slots go:
//
//   chip        up to four round slots, two by two, each captioned
//   arc         up to four bands round the bezel - top, right, bottom, left - listed in the middle
//   background  one slot filling the face
//
// A case names the complication type it is fed and the data it carries:
//
//   text, title          strings
//   icon                 a monochromatic glyph, tinted by the face
//   smallImage           'ICON' or 'PHOTO', an image that keeps its own colours
//   photo                a photograph, for PHOTO_IMAGE
//   value, min, max      a RANGED_VALUE; value and target for a GOAL_PROGRESS
//   colors, interpolate  the provider's colour ramp
//   elements             WEIGHTED_ELEMENTS, as [weight, colour] pairs
//
// and optionally how the slot is declared (slot: attributes of com.xlythe.ComplicationSlot, such
// as complicationDrawableStyle, color or direction). A case with no type is left EMPTY.
//
// Some branches of the layouts cannot be reached from here: Jetpack will not build a SHORT_TEXT
// or LONG_TEXT without text, so the title-only and icon-only arrangements of those types are
// never drawn by a real provider either.

String BLUE = '#FF4285F4', GREEN = '#FF34A853', YELLOW = '#FFFBBC05', RED = '#FFEA4335', PURPLE = '#FFA142F4'
List<List<Object>> FOUR = [[4, BLUE], [3, GREEN], [2, YELLOW], [1, RED]]

return [
        // Chips, one type at a time.
        [name: 'chip_short_text', title: 'SHORT_TEXT', layout: 'chip', cases: [
                [caption: 'text', type: 'SHORT_TEXT', text: '72°'],
                [caption: 'text + title', type: 'SHORT_TEXT', text: '72°', title: 'Sunny'],
                [caption: 'text + icon', type: 'SHORT_TEXT', text: '7:30', icon: true],
                [caption: 'all three', type: 'SHORT_TEXT', text: '7:30', title: 'Alarm', icon: true],
        ]],
        [name: 'chip_long_text', title: 'LONG_TEXT', layout: 'chip', cases: [
                [caption: 'text', type: 'LONG_TEXT', text: 'Standup in 10 min'],
                [caption: 'title + text', type: 'LONG_TEXT', text: 'Standup in 10 min', title: 'Calendar'],
                [caption: 'icon + text', type: 'LONG_TEXT', text: 'Standup in 10 min', icon: true],
                [caption: 'image + text', type: 'LONG_TEXT', text: 'Standup in 10 min', smallImage: 'ICON'],
        ]],
        [name: 'chip_ranged', title: 'RANGED_VALUE', layout: 'chip', cases: [
                [caption: 'no text', type: 'RANGED_VALUE', value: 60, min: 0, max: 100],
                [caption: 'text', type: 'RANGED_VALUE', value: 60, min: 0, max: 100, text: '60%'],
                [caption: 'text + title', type: 'RANGED_VALUE', value: 60, min: 0, max: 100, text: '60%', title: 'Battery'],
                [caption: 'icon + text', type: 'RANGED_VALUE', value: 60, min: 0, max: 100, text: '60%', icon: true],
        ]],
        [name: 'chip_ranged_more', title: 'RANGED_VALUE', layout: 'chip', cases: [
                [caption: 'icon', type: 'RANGED_VALUE', value: 25, min: 0, max: 100, icon: true],
                [caption: 'min -10, max 40', type: 'RANGED_VALUE', value: 25, min: -10, max: 40],
                [caption: 'smooth colours', type: 'RANGED_VALUE', value: 70, min: 0, max: 100, text: '70%',
                 colors: [GREEN, YELLOW, RED], interpolate: true],
                [caption: 'stepped colours', type: 'RANGED_VALUE', value: 70, min: 0, max: 100, text: '70%',
                 colors: [GREEN, YELLOW, RED], interpolate: false],
        ]],
        [name: 'chip_goal', title: 'GOAL_PROGRESS', layout: 'chip', cases: [
                [caption: 'no text', type: 'GOAL_PROGRESS', value: 7000, target: 10000],
                [caption: 'text + title', type: 'GOAL_PROGRESS', value: 7000, target: 10000, text: '7,000', title: 'Steps'],
                [caption: 'icon + text', type: 'GOAL_PROGRESS', value: 7000, target: 10000, text: '7,000', icon: true],
                [caption: 'passed', type: 'GOAL_PROGRESS', value: 13000, target: 10000, text: '13,000'],
        ]],
        [name: 'chip_goal_more', title: 'GOAL_PROGRESS', layout: 'chip', cases: [
                [caption: 'icon', type: 'GOAL_PROGRESS', value: 7000, target: 10000, icon: true],
                [caption: 'text', type: 'GOAL_PROGRESS', value: 7000, target: 10000, text: '7k'],
                [caption: 'nothing yet', type: 'GOAL_PROGRESS', value: 0, target: 10000, text: '0'],
                [caption: 'colours', type: 'GOAL_PROGRESS', value: 7000, target: 10000, text: '7k',
                 colors: [BLUE, GREEN], interpolate: true],
        ]],
        [name: 'chip_weighted', title: 'WEIGHTED_ELEMENTS', layout: 'chip', cases: [
                [caption: 'no text', type: 'WEIGHTED_ELEMENTS', elements: FOUR],
                [caption: 'text', type: 'WEIGHTED_ELEMENTS', elements: FOUR, text: '7h'],
                [caption: 'text + title', type: 'WEIGHTED_ELEMENTS', elements: FOUR, text: '7h', title: 'Sleep'],
                [caption: 'icon', type: 'WEIGHTED_ELEMENTS', elements: FOUR, icon: true],
        ]],
        [name: 'chip_weighted_more', title: 'WEIGHTED_ELEMENTS', layout: 'chip', cases: [
                [caption: 'eight elements', type: 'WEIGHTED_ELEMENTS', text: '7h',
                 elements: [[8, BLUE], [7, GREEN], [6, YELLOW], [5, RED], [4, PURPLE], [3, BLUE], [2, GREEN], [1, YELLOW]]],
                [caption: 'one element', type: 'WEIGHTED_ELEMENTS', elements: [[1, BLUE]], text: '7h'],
                [caption: 'two elements', type: 'WEIGHTED_ELEMENTS', elements: [[3, BLUE], [1, GREEN]], text: '7h'],
                [caption: 'five elements', type: 'WEIGHTED_ELEMENTS', text: '7h',
                 elements: [[5, BLUE], [4, GREEN], [3, YELLOW], [2, RED], [1, PURPLE]]],
        ]],
        [name: 'chip_images', title: 'IMAGES AND EMPTY', layout: 'chip', cases: [
                [caption: 'MONOCHROMATIC_IMAGE', type: 'MONOCHROMATIC_IMAGE', icon: true],
                [caption: 'SMALL_IMAGE icon', type: 'SMALL_IMAGE', smallImage: 'ICON'],
                [caption: 'SMALL_IMAGE photo', type: 'SMALL_IMAGE', smallImage: 'PHOTO'],
                [caption: 'EMPTY'],
        ]],
        [name: 'chip_styles', title: 'DRAWABLE STYLES', layout: 'chip', cases: [
                [caption: 'fill', type: 'SHORT_TEXT', text: '72°', title: 'Sunny',
                 slot: [complicationDrawableStyle: 'fill', contentColor: '#FF000000']],
                [caption: 'dot', type: 'SHORT_TEXT', text: '72°', title: 'Sunny', slot: [complicationDrawableStyle: 'dot']],
                [caption: 'empty', type: 'SHORT_TEXT', text: '72°', title: 'Sunny', slot: [complicationDrawableStyle: 'empty']],
                [caption: 'coloured', type: 'RANGED_VALUE', value: 60, min: 0, max: 100, text: '60%',
                 slot: [color: '#FF8AB4F8', ambientColor: '#FFFFFFFF']],
        ]],

        // Bands round the bezel.
        [name: 'arc_text', title: 'TEXT', layout: 'arc', cases: [
                [caption: 'SHORT_TEXT', type: 'SHORT_TEXT', text: 'Thu 24'],
                [caption: 'LONG_TEXT', type: 'LONG_TEXT', text: 'Standup in 10 min'],
                [caption: 'SHORT_TEXT, anticlockwise', type: 'SHORT_TEXT', text: '72° Sunny',
                 slot: [direction: 'COUNTER_CLOCKWISE']],
                [caption: 'LONG_TEXT + title, anticlockwise', type: 'LONG_TEXT', text: 'Standup in 10 min',
                 title: 'Calendar', slot: [direction: 'COUNTER_CLOCKWISE']],
        ]],
        [name: 'arc_ranged', title: 'RANGED_VALUE', layout: 'arc', cases: [
                [caption: '0%', type: 'RANGED_VALUE', value: 0, min: 0, max: 100],
                [caption: '15%', type: 'RANGED_VALUE', value: 15, min: 0, max: 100],
                [caption: '60%', type: 'RANGED_VALUE', value: 60, min: 0, max: 100],
                [caption: '100%', type: 'RANGED_VALUE', value: 100, min: 0, max: 100],
        ]],
        [name: 'arc_ranged_more', title: 'RANGED_VALUE', layout: 'arc', cases: [
                [caption: 'icon', type: 'RANGED_VALUE', value: 60, min: 0, max: 100, icon: true],
                [caption: 'min -10, max 40', type: 'RANGED_VALUE', value: 25, min: -10, max: 40],
                [caption: 'colours', type: 'RANGED_VALUE', value: 70, min: 0, max: 100,
                 colors: [GREEN, YELLOW, RED], interpolate: true],
                [caption: 'coloured slot', type: 'RANGED_VALUE', value: 60, min: 0, max: 100,
                 slot: [color: '#FF8AB4F8', ambientColor: '#FFFFFFFF']],
        ]],
        [name: 'arc_goal', title: 'GOAL_PROGRESS', layout: 'arc', cases: [
                [caption: '0%', type: 'GOAL_PROGRESS', value: 0, target: 10000],
                [caption: '70%', type: 'GOAL_PROGRESS', value: 7000, target: 10000],
                [caption: '100%', type: 'GOAL_PROGRESS', value: 10000, target: 10000],
                [caption: '130%', type: 'GOAL_PROGRESS', value: 13000, target: 10000],
        ]],
        [name: 'arc_weighted', title: 'WEIGHTED_ELEMENTS', layout: 'arc', cases: [
                [caption: '1 element', type: 'WEIGHTED_ELEMENTS', elements: [[1, BLUE]]],
                [caption: '2 elements', type: 'WEIGHTED_ELEMENTS', elements: [[2, BLUE], [1, GREEN]]],
                [caption: '3 elements', type: 'WEIGHTED_ELEMENTS', elements: [[3, BLUE], [2, GREEN], [1, YELLOW]]],
                [caption: '5 elements', type: 'WEIGHTED_ELEMENTS',
                 elements: [[5, BLUE], [4, GREEN], [3, YELLOW], [2, RED], [1, PURPLE]]],
        ]],
        [name: 'arc_images', title: 'IMAGES AND EMPTY', layout: 'arc', cases: [
                [caption: 'MONOCHROMATIC_IMAGE', type: 'MONOCHROMATIC_IMAGE', icon: true],
                [caption: 'SMALL_IMAGE icon', type: 'SMALL_IMAGE', smallImage: 'ICON'],
                [caption: 'SMALL_IMAGE photo', type: 'SMALL_IMAGE', smallImage: 'PHOTO'],
                [caption: 'EMPTY'],
        ]],
        [name: 'arc_styles', title: 'DRAWABLE STYLES', layout: 'arc', cases: [
                [caption: 'line', type: 'SHORT_TEXT', text: 'Thu 24'],
                [caption: 'fill', type: 'SHORT_TEXT', text: 'Thu 24',
                 slot: [complicationDrawableStyle: 'fill', contentColor: '#FF000000']],
                [caption: 'dot', type: 'SHORT_TEXT', text: 'Thu 24', slot: [complicationDrawableStyle: 'dot']],
                [caption: 'empty', type: 'SHORT_TEXT', text: 'Thu 24', slot: [complicationDrawableStyle: 'empty']],
        ]],

        // The whole face.
        [name: 'background_photo', title: 'PHOTO_IMAGE', layout: 'background', cases: [
                [caption: 'PHOTO_IMAGE', type: 'PHOTO_IMAGE', photo: true],
        ]],
        [name: 'background_small_image', title: 'SMALL_IMAGE', layout: 'background', cases: [
                [caption: 'SMALL_IMAGE', type: 'SMALL_IMAGE', smallImage: 'ICON'],
        ]],
        [name: 'background_monochromatic', title: 'MONOCHROMATIC_IMAGE', layout: 'background', cases: [
                [caption: 'MONOCHROMATIC_IMAGE', type: 'MONOCHROMATIC_IMAGE', icon: true],
        ]],
        [name: 'background_empty', title: 'EMPTY', layout: 'background', cases: [
                [caption: 'EMPTY'],
        ]],
]
