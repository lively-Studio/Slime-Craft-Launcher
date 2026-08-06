package studio.lively.scl.ui.skin.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import studio.lively.scl.ui.skin.FunctionHelper;
import studio.lively.scl.ui.skin.SkinCanvas;
import studio.lively.scl.ui.skin.SkinCanvasSupport;
import studio.lively.scl.ui.skin.animation.SkinAniRunning;
import studio.lively.scl.ui.skin.animation.SkinAniWavingArms;
import studio.lively.scl.game.TexturesLoader;

import java.util.function.Consumer;

public class Test extends Application {

    public static final String TITLE = "FX - Minecraft skin preview";

    public static SkinCanvas createSkinCanvas() {
        SkinCanvas canvas = new SkinCanvas(TexturesLoader.getDefaultSkinImage(), 400, 400, true);
        canvas.getAnimationPlayer().addSkinAnimation(new SkinAniWavingArms(100, 2000, 7.5, canvas), new SkinAniRunning(100, 100, 30, canvas));
        FunctionHelper.alwaysB(Consumer<SkinCanvas>::accept, canvas, new SkinCanvasSupport.Mouse(.5), new SkinCanvasSupport.Drag(TITLE));
        return canvas;
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle(TITLE);
        Scene scene = new Scene(createSkinCanvas());
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String... args) {
        launch(args);
    }

}
