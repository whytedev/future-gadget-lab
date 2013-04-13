
import java.util.Collection;
import java.util.LinkedList;

import bonzai.api.AI;
import bonzai.api.Duck;
import bonzai.api.Farmhand;
import bonzai.api.FarmhandAction;
import bonzai.api.GameState;

public class BasicAI implements AI {

	@Override
	public Collection<FarmhandAction> turn(GameState state) {
		// The set of actions to perform
		LinkedList<FarmhandAction> actions = new LinkedList<FarmhandAction>();

		// Get all farmhands on my team that aren't stumbled
		for (Farmhand farmhand : state.getMyFarmhands().getNotStumbled()) {
			// If holding a duck
			if (farmhand.getHeldObject() instanceof Duck) {
				int dx = state.getMyBase().getX() - farmhand.getX();
				int dy = state.getMyBase().getY() - farmhand.getY();
				// If not adjacent to base
				if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
					// Move towards base
					actions.add(farmhand.move(
							farmhand.getX() + (int) Math.signum(dx),
							farmhand.getY() + (int) Math.signum(dy)));
				} else {
					// Drop on base
					actions.add(farmhand.dropItem(state.getMyBase()
							.getPosition()));
				}
			} else {
				// Get the closest visible duck owned by my team
				Duck closestDuck = state.getMyDucks().getNotHeld()
						.getClosestTo(farmhand);
				if (closestDuck != null) {

					int dx = farmhand.getX() - closestDuck.getX();
					int dy = farmhand.getY() - closestDuck.getY();

					// If not adjacent to the duck
					if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {

						int newX = farmhand.getX()
								+ (int) Math.signum(closestDuck.getX()
										- farmhand.getX());
						int newY = farmhand.getY()
								+ (int) Math.signum(closestDuck.getY()
										- farmhand.getY());
						// Move closer to the duck, if the tile is crossable
						if (state.isTileEmpty(newX, newY)
								&& state.getTile(newX, newY).canFarmhandCross()) {
							actions.add(farmhand.move(newX, newY));
						}
					} else {
						// Pick up the duck
						actions.add(farmhand.pickUp(closestDuck));
					}
				}
			}
		}

		return actions;

	}
}
