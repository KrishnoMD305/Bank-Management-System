package com.bank;

import com.bank.service.BankService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Application entry point.
 *
 * On startup this also boots the BankService's background interest-accrual
 * scheduler (a ScheduledExecutorService running on its own daemon thread),
 * and on close it shuts down all background thread pools cleanly.
 */
public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // Start background concurrency services (interest accrual thread)
        BankService.getInstance().startInterestScheduler();

        switchScene("/fxml/Login.fxml", "Bank Management System - Admin Login");

        stage.setOnCloseRequest(e -> BankService.getInstance().shutdown());
        stage.setResizable(true);
        stage.show();
    }

    /**
     * Loads a new FXML scene into the primary stage. Used by controllers to
     * navigate between screens (Login -> Register -> Dashboard, etc).
     */
    public static void switchScene(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root);

        var cssUrl = Main.class.getResource("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
