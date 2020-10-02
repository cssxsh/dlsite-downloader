package xyz.cssxsh.dlsite

import javafx.application.Application
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.scene.control.Button
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class Downloader : Application() {

    lateinit var flush: Button

    lateinit var table : TableView<WorkInfo>

    private val jMetro: jfxtras.styles.jmetro.JMetro by lazy {
        jfxtras.styles.jmetro.JMetro(jfxtras.styles.jmetro.Style.valueOf(DLsiteTool.config.style))
    }

    override fun start(primaryStage: Stage) {
        runBlocking {
            DLsiteTool.login()
        }
        primaryStage.title = "SID:${DLsiteTool.config.loginId}"
        primaryStage.scene = Scene(FXMLLoader.load(javaClass.getResource("/Downloader.fxml"))).also {
            jMetro.scene = it
        }
        primaryStage.show()
    }

    @Suppress("unused")
    fun onFlush(actionEvent: ActionEvent) {
        table.columns.forEach { column ->
            column.cellValueFactory = PropertyValueFactory(column.text)
        }
        GlobalScope.launch {
            DLsiteTool.purchases { data ->
                table.items.addAll(data)
            }
        }
    }

    @Suppress("unused")
    fun onDownload(actionEvent: ActionEvent) {
        //
    }
}