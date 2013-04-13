import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import bonzai.api.AI;
import bonzai.api.Farmhand;
import bonzai.api.FarmhandAction;
import bonzai.api.GameState;


public class CompetitorAI implements AI {
	
	//Codes for role assignment
	private final int R_DUCK_FETCH = 0;
	private final int R_RECON = 1;
	private final int R_GRIEF = 2;
	private final int R_PATHS = 3;
	
	private HashMap<Integer, Integer> roles = new HashMap<Integer, Integer>();
	
	@Override
	public Collection<FarmhandAction> turn(GameState state) {
		ArrayList<FarmhandAction> actions = new ArrayList<FarmhandAction>();
		
		//Number of workers to assign to each role
		int totalWorkers = state.getMyFarmhands().size();
		int duckFetch = totalWorkers/2;
		int recon = 1;
		int grief = 1;
		int pathbuilding = totalWorkers - duckFetch - recon - grief;
		
		//role assignment
		for (int index = 0; index < totalWorkers; index++) {
			if (duckFetch > 0) {
				roles.put(index, R_DUCK_FETCH);
				duckFetch--;
			}
			else if (recon > 0) {
				roles.put(index,  R_RECON);
				recon--;
			}
			else if (grief > 0) {
				roles.put(index, R_GRIEF);
				grief--;
			}
			else if (pathbuilding > 0) {
				roles.put(index, R_PATHS);
				pathbuilding--;
			}
		}
		
		/* Print out what each farmhand is going to do for this turn
		for (Integer i : roles.keySet()) {
			System.out.println("Farmhand " + i + " is doing " + roles.get(i));
		}
		*/
		
		int index = 0;
		for (Farmhand farmhand : state.getMyFarmhands()) {
			if (roles.get(index) == R_DUCK_FETCH)
				actions.add(duckFetch(state, farmhand));
			else if (roles.get(index) == R_RECON)
				actions.add(recon(state, farmhand));
			else if (roles.get(index) == R_GRIEF)
				actions.add(grief(state, farmhand));
			else if (roles.get(index) == R_PATHS)
				actions.add(buildPaths(state, farmhand));
			else
				actions.add(noJob(state, farmhand));
		}
		
		return actions;
	}

	private FarmhandAction noJob(GameState state, Farmhand farmhand) {
		return farmhand.shout("I'm lazy and have no job");
	}

	private FarmhandAction buildPaths(GameState state, Farmhand farmhand) {
		return farmhand.shout("I'm building paths");
	}

	private FarmhandAction grief(GameState state, Farmhand farmhand) {
		return farmhand.shout("I'm griefing");
	}

	private FarmhandAction recon(GameState state, Farmhand farmhand) {
		return farmhand.shout("I'm reconing");
	}

	private FarmhandAction duckFetch(GameState state, Farmhand farmhand) {
		return farmhand.shout("I'm fetching ducks");
	}
}
