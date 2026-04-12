package com.erp.ui;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Teste mínimo para verificar se TestFX + Monocle funciona neste ambiente.
 * Não usa Spring nem StageManager.
 */
@ExtendWith(ApplicationExtension.class)
class MinimalUITest extends FxRobot {

    @Start
    void start(Stage stage) {
        TextField txt = new TextField();
        txt.setId("txtInput");

        Label lbl = new Label("Inicial");
        lbl.setId("lblResult");

        Button btn = new Button("Clicar");
        btn.setId("btnTest");
        btn.setOnAction(e -> lbl.setText("Clicado!"));

        VBox root = new VBox(txt, btn, lbl);
        stage.setScene(new Scene(root, 400, 300));
        stage.show();
    }

    @Test
    void dado_botao_quando_clicar_entao_label_muda() {
        clickOn("#btnTest");
        FxAssert.verifyThat("#lblResult", LabeledMatchers.hasText(equalTo("Clicado!")));
    }

    @Test
    void dado_campo_quando_digitar_entao_contem_texto() {
        clickOn("#txtInput").write("Olá TestFX");
        assertThat(lookup("#txtInput").queryAs(TextField.class).getText())
            .isEqualTo("Olá TestFX");
    }
}
