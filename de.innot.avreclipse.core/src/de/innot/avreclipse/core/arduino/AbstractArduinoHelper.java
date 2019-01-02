package de.innot.avreclipse.core.arduino;

import java.util.List;

public abstract class AbstractArduinoHelper {

	public abstract List<String> findArduinoPorts(ArduinoBoards boards, String boardId);
}
