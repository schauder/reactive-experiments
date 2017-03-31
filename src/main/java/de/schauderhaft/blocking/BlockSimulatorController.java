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

import java.time.Duration;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

/**
 * @author Jens Schauder
 */
public class BlockSimulatorController {

	@FXML private TextField duration;

	@FXML private TextField percentageDbRequests;

	@FXML private TextField numberOfMainThreads;

	@FXML private TextField numberOfDbThreads;

	@FXML private LineChart<Duration, Integer> chart;

	@FXML
	protected void handleStopButtonAction(ActionEvent event) {
		System.out.println("Stop");
	}

	@FXML
	protected void handleStartButtonAction(ActionEvent event) {
		System.out.println("Start");
		System.out.println(duration.getText());
	}
}
