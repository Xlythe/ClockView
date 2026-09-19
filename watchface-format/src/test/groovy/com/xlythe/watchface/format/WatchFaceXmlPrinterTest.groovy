package com.xlythe.watchface.format

import org.junit.Test

import static org.junit.Assert.assertEquals

class WatchFaceXmlPrinterTest {
    @Test
    void print_indentsElementsAndKeepsTextAgainstItsTags() {
        // Expanding normalizes text whitespace, as the generate task does before printing.
        Node root = new ComplicationSlotExpander([:], '#FFFFFFFF', '#FFFFFFFF').expand(TemplateProcessor.parse('''
            <WatchFace width="450" height="450">
                <Scene>
                    <Condition>
                        <Expressions>
                            <Expression name="c">
                                [HOUR_0_23] &lt; 12
                            </Expression>
                        </Expressions>
                    </Condition>
                    <PartText x="0" y="0" width="100" height="40">
                        <Text>
                            <Font family="SYNC_TO_DEVICE" size="20">
                                <Template>%s°<Parameter expression="[WEATHER.TEMPERATURE]" /></Template>
                            </Font>
                        </Text>
                    </PartText>
                    <PartImage x="0" y="0" width="1" height="1"><Image resource="dot" /></PartImage>
                </Scene>
            </WatchFace>'''))

        assertEquals('''<WatchFace width="450" height="450">
  <Scene>
    <Condition>
      <Expressions>
        <Expression name="c">[HOUR_0_23] &lt; 12</Expression>
      </Expressions>
    </Condition>
    <PartText x="0" y="0" width="100" height="40">
      <Text>
        <Font family="SYNC_TO_DEVICE" size="20">
          <Template>%s°<Parameter expression="[WEATHER.TEMPERATURE]"/></Template>
        </Font>
      </Text>
    </PartText>
    <PartImage x="0" y="0" width="1" height="1">
      <Image resource="dot"/>
    </PartImage>
  </Scene>
</WatchFace>
''', TemplateProcessor.print(root))
    }

    @Test
    void print_escapesTextAndAttributes() {
        Node root = TemplateProcessor.parse('<Expression name="a &quot;b&quot;">x &amp;&amp; y &gt; 2</Expression>')

        assertEquals('<Expression name="a &quot;b&quot;">x &amp;&amp; y &gt; 2</Expression>\n', TemplateProcessor.print(root))
    }
}
