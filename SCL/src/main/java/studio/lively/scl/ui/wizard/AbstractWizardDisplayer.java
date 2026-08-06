/*
 * Slime Craft Launcher
 * Copyright (C) 2020  lively-Studio <X_CODER_ocs2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package studio.lively.scl.ui.wizard;

import javafx.scene.control.Label;
import studio.lively.scl.task.Schedulers;
import studio.lively.scl.task.Task;
import studio.lively.scl.task.TaskExecutor;
import studio.lively.scl.ui.construct.TaskListPane;
import studio.lively.scl.util.SettingsMap;

import java.util.Queue;

public abstract class AbstractWizardDisplayer implements WizardDisplayer {
    private final Queue<Object> cancelQueue;

    public AbstractWizardDisplayer(Queue<Object> cancelQueue) {
        this.cancelQueue = cancelQueue;
    }

    @Override
    public void handleTask(SettingsMap settings, Task<?> task) {
        TaskExecutor executor = task.withRunAsync(Schedulers.javafx(), this::navigateToSuccess).executor();
        TaskListPane pane = new TaskListPane();
        pane.setExecutor(executor);
        navigateTo(pane, Navigation.NavigationDirection.FINISH);
        cancelQueue.add(executor);
        executor.start();
    }

    @Override
    public void onCancel() {
        while (!cancelQueue.isEmpty()) {
            Object x = cancelQueue.poll();
            if (x instanceof TaskExecutor) ((TaskExecutor) x).cancel();
            else if (x instanceof Thread) ((Thread) x).interrupt();
            else throw new IllegalStateException("Unrecognized cancel queue element: " + x);
        }
    }

    void navigateToSuccess() {
        navigateTo(new Label("Successful"), Navigation.NavigationDirection.FINISH);
    }
}
