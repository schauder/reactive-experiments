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
import javafx.scene.control.TextField;

/**
 * @author Jens Schauder
 */
public class BlockSimulatorController implements Initializable {

	@FXML private TextField percentageDbRequests;

	@FXML private TextField numberOfMainThreads;

	@FXML private TextField numberOfDbThreads;

	@FXML private LineChart<Long, Long> chart;

	private Experiment experiment;

	@FXML
	protected void handleStopButtonAction(ActionEvent event) {
		stop();
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
				percentageDbCalls = Integer.valueOf(percentageDbRequests.getText());
				dbThreads = Integer.valueOf(numberOfDbThreads.getText());
				mainThreads = Integer.valueOf(numberOfMainThreads.getText());
			}
		});
		experiment.run(t -> {

			System.out.println(String.format("%s %s %s", t.getT1(), t.getT2(), t.getT3()));
			Platform.runLater(() -> {
				if (offset[0] == null) {
					offset[0] = t.getT1();
				}
				countsByType.get(t.getT2()).add(new Data<>(t.getT1() - offset[0], t.getT3()));
			});
		});
	}

	private Map<Type, List<Data<Long, Long>>> createSeries() {
		Series<Long, Long> dbCalls = new Series<>();
		dbCalls.setName("# Requests (DB)");

		Series<Long, Long> compCalls = new Series<>();
		compCalls.setName("# Requests (Comp)");

		chart.getData().add(dbCalls);
		chart.getData().add(compCalls);

		Map<Type, List<Data<Long, Long>>> countsByType = new HashMap<>();
		countsByType.put(Type.DB, dbCalls.getData());
		countsByType.put(Type.COMPUTATIONAL, compCalls.getData());
		return countsByType;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		percentageDbRequests.setText("2");
		numberOfMainThreads.setText("4");
		numberOfDbThreads.setText("4");
	}
}
