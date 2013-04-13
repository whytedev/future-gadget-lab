import java.util.ArrayList;
import java.util.Collection;

import bonzai.api.AI;
import bonzai.api.Farmhand;
import bonzai.api.FarmhandAction;
import bonzai.api.GameState;


public class CompetitorAI implements AI {

	@Override
	public Collection<FarmhandAction> turn(GameState state) {
		ArrayList<FarmhandAction> actions = new ArrayList<FarmhandAction>();
		
		for (Farmhand farmhand : state.getMyFarmhands().getNotStumbled()) {
			actions.add(farmhand.shout("Hello"));
		}
		
		return actions;
	}

}
