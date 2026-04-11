package com.erp.util;

import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Converte arquivos SVG do classpath em JavaFX {@link Image}.
 *
 * <p>JavaFX suporta apenas formatos raster (PNG, JPG, GIF, BMP) na classe {@code Image}.
 * Esta classe usa o Apache Batik para transcodificar o SVG em PNG em memória,
 * retornando um {@code Image} pronto para uso em {@code ImageView}.</p>
 *
 * <p>Renderiza em 2× a resolução de exibição para suporte a telas HiDPI.</p>
 */
@Slf4j
@Component
public class SvgImageLoader {

    /**
     * Carrega um SVG do classpath e retorna um {@link Image} JavaFX.
     *
     * @param resourcePath caminho absoluto no classpath, ex: {@code "/images/logo-dark.svg"}
     * @param displayWidth largura de exibição em pixels lógicos
     * @param displayHeight altura de exibição em pixels lógicos
     * @return {@link Image} renderizada, ou {@code null} em caso de erro
     */
    public Image load(String resourcePath, float displayWidth, float displayHeight) {
        try {
            var url = getClass().getResource(resourcePath);
            if (url == null) {
                log.error("SVG não encontrado no classpath: {}", resourcePath);
                return null;
            }

            var transcoder = new PNGTranscoder();
            // Renderiza em 2× para telas HiDPI (Retina / 4K)
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH,  displayWidth  * 2f);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, displayHeight * 2f);

            var input  = new TranscoderInput(url.toString());
            var baos   = new ByteArrayOutputStream();
            var output = new TranscoderOutput(baos);

            transcoder.transcode(input, output);

            return new Image(new ByteArrayInputStream(baos.toByteArray()));

        } catch (Exception e) {
            log.error("Erro ao converter SVG '{}' para Image: {}", resourcePath, e.getMessage(), e);
            return null;
        }
    }
}
