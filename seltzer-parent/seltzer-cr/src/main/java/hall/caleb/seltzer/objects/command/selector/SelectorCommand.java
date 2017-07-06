package hall.caleb.seltzer.objects.command.selector;

import java.util.UUID;

import hall.caleb.seltzer.enums.CommandType;
import hall.caleb.seltzer.enums.SelectorType;
import hall.caleb.seltzer.objects.command.Command;
import hall.caleb.seltzer.objects.command.Selector;

public class SelectorCommand extends Command {
	protected Selector selector;
	
	public SelectorCommand() {
		super();
	}

	public SelectorCommand(CommandType commandType) {
		super(commandType);
	}
	
	public SelectorCommand(CommandType commandType, UUID id) {
		super(commandType, id);
	}

	public void setSelector(String selector, SelectorType selectorType) {
		this.selector.setSelector(selectorType, selector);
	}
	
	@Override
	public String toString() {
		return "SelectorCommand [selector=" + selector + ", USES_COMMAND_LIST=" + USES_COMMAND_LIST + ", type=" + type
				+ ", id=" + id + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((selector == null) ? 0 : selector.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SelectorCommand other = (SelectorCommand) obj;
		if (selector == null) {
			if (other.selector != null)
				return false;
		} else if (!selector.equals(other.selector))
			return false;
		return true;
	}

	public Selector getSelector() {
		return selector;
	}

	public void setSelector(Selector selector) {
		this.selector = selector;
	}
}