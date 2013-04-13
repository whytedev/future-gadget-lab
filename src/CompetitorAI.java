import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import bonzai.api.AI;
import bonzai.api.Duck;
import bonzai.api.Farmhand;
import bonzai.api.FarmhandAction;
import bonzai.api.GameState;
import bonzai.api.Item;
import bonzai.api.Position;
import bonzai.api.Tile;
import bonzai.api.Item.Type;
import bonzai.api.list.DuckList;


public class CompetitorAI implements AI {

	//Codes for role assignment
	private final int R_DUCK_FETCH = 0;
	private final int R_RECON = 1;
	private final int R_GRIEF = 2;
	private final int R_PATHS = 3;
	boolean boughtBucket = false;

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
				//System.out.println("Adding Fetcher " + duckFetch);
				duckFetch--;
			}
			else if (recon > 0) {
				//System.out.println("Adding Recon " + recon);
				roles.put(index,  R_RECON);
				recon--;
			}
			else if (grief > 0) {
				//System.out.println("Adding grief " + grief);
				roles.put(index, R_GRIEF);
				grief--;
			}
			else if (pathbuilding > 0) {
				//System.out.println("Adding Pathbuilder" + pathbuilding);
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
			else if (roles.get(index) == R_GRIEF && !farmhand.isStumbled())
				actions.add(grief(state, farmhand));
			else if (roles.get(index) == R_PATHS)
				actions.add(buildPaths(state, farmhand));
			else
				actions.add(noJob(state, farmhand));
			index++;
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
		// Get the closest visible duck owned by other team
		DuckList ducks = state.getDucks().getNotHeld();
		ducks.removeAll(state.getMyDucks());
		Duck closestDuck = ducks.getClosestTo(farmhand);
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
					return farmhand.move(newX, newY);
				}
			} 
		}
		return farmhand.shout("I'm griefing");
	}

	private FarmhandAction recon(GameState state, Farmhand farmhand) {
		return farmhand.shout("I'm reconing");
	}

	private FarmhandAction duckFetch(GameState state, Farmhand farmhand) {
		return farmhand.shout("I'm fetching ducks");
	}
}
