/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schauderhaft.blocking;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import de.schauderhaft.blocking.Request.Type;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

/**
 * @author Jens Schauder
 */
public class BlockSimulatorController implements Initializable {

	@FXML private TextField delayBetweenEvents;
	@FXML private TextField dbTaskDuration;

	@FXML private TextField percentageDbRequests;

	@FXML private TextField numberOfMainThreads;
	@FXML private TextField numberOfDbThreads;

	@FXML private CheckBox workShedding;

	@FXML private LineChart<Long, Long> chartCalc;
	@FXML private LineChart<Long, Long> chartDb;
	@FXML private LineChart<Long, Long> chartDrop;

	private Map<Type, LineChart<Long, Long>> charts = new HashMap<>();

	private Experiment experiment;

	@FXML
	protected void handleStopButtonAction(ActionEvent event) {
		stop();
	}

	@FXML
	protected void handleClearButtonAction(ActionEvent event) {
		for (LineChart<Long, Long> chart : charts.values()) {

			chart.getData().clear();
		}
	}

	private void stop() {
		if (experiment != null) experiment.dispose();
	}

	@FXML
	protected void handleStartButtonAction(ActionEvent event) {
		stop();

		Map<Type, List<Data<Long, Long>>> countsByType = createSeries();

		final Long[] offset = {null};
		experiment = new Experiment(new Configuration() {
			{

				delay = Integer.valueOf(delayBetweenEvents.getText());
				duration = Integer.valueOf(dbTaskDuration.getText());
				percentageDbCalls = Integer.valueOf(percentageDbRequests.getText());
				dbThreads = Integer.valueOf(numberOfDbThreads.getText());
				mainThreads = Integer.valueOf(numberOfMainThreads.getText());
				shedWork = workShedding.isSelected();
			}
		});
		experiment.run(measurement -> {

			Platform.runLater(() -> {
				if (offset[0] == null) {
					offset[0] = measurement.getTimestamp();
				}

				addAndReplace(countsByType, offset, measurement);
			});
		});
	}

	private void addAndReplace(Map<Type, List<Data<Long, Long>>> countsByType, Long[] offset, Measurement measurement) {
		long correctedTimestamp = measurement.getTimestamp() - offset[0];
		Long oldValue = 0L;
		List<Data<Long, Long>> series = countsByType.get(measurement.getRequestType());
		for (Data<Long, Long> data : series) {
			if (data.getXValue().equals(correctedTimestamp)) {
				oldValue = data.getYValue();
				series.remove(data);
				break;
			}
		}
		Data<Long, Long> newData = new Data<>(correctedTimestamp, measurement.getValue() + oldValue);
		series.add(newData);
	}

	private Map<Type, List<Data<Long, Long>>> createSeries() {

		Map<Type, List<Data<Long, Long>>> countsByType = new HashMap<>();

		for (Type type : Type.values()) {
			Series<Long, Long> callsByType = new Series<>();
			callsByType.setName(String.format("# Requests (%s)", type));
			charts.get(type).getData().add(callsByType);

			countsByType.put(type, callsByType.getData());
		}

		return countsByType;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		delayBetweenEvents.setText("1");
		dbTaskDuration.setText("300");
		percentageDbRequests.setText("20");
		numberOfMainThreads.setText("4");
		numberOfDbThreads.setText("1");

		charts.put(Type.COMPUTATIONAL, chartCalc);
		charts.put(Type.DB, chartDb);
		charts.put(Type.DROPPED, chartDrop);
	}
}
