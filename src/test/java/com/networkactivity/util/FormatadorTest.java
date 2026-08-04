package com.networkactivity.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FormatadorTest {

    @Test
    void deveFormatarZeroComoZeroBytesPorSegundo() {
        assertEquals("0,0 B/s", Formatador.formatarVelocidade(0));
    }

    @Test
    void deveFormatarComoKilobytesQuandoPassarDeMil() {
        assertEquals("1,5 KB/s", Formatador.formatarVelocidade(1536));
    }

    @Test
    void deveFormatarComoMegabytesQuandoPassarDeUmMilhao() {
        assertEquals("2,0 MB/s", Formatador.formatarVelocidade(2.0 * 1024 * 1024));
    }
    @Test
    void deveFormatarComGigaBytesQuandoPassarDeUmBilhao() {
    	assertEquals("1,0 GB/s", Formatador.formatarVelocidade(1024 * 1024 * 1024));
    }
}
