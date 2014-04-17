package target.eyes.vag.codec.xml.javolution;

public class VASTv2Constants {

	/**
	 * VAST v2 event types
	 */

	public static enum EventType {
		creativeView("creativeView"), start("start"), midpoint("midpoint"), firstQuartile(
				"firstQuartile"), thirdQuartile("thirdQuartile"), complete(
				"complete"), mute("mute"), unmute("unmute"), pause("pause"), rewind(
				"rewind"), resume("resume"), fullscreen("fullscreen"), expand(
				"expand"), collapse("collapse"), acceptInvitation(
				"acceptInvitation"), click("click"), close("close");

		public final String stringValue;

		private EventType(String stringValue) {
			this.stringValue = stringValue;
		}

		@Override
		public String toString() {
			return stringValue;
		}
	}

}
