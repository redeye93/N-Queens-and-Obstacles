/*
@Assignment : Artificial Intelligence assignment 2
@Aurthor Utkarsh Gera

@Explaination
The program reads an input file and assigns the values to the variables as mentioned in the problem. After detecting which type of search to use,
it triggers the algorithm and returns the solution if any. Since this problem is a modified version of N-Queens, direct algorithm cannot be used.

Brute force approach is used with few heuristics so as to optimize the algorithm. Even HashMap is used to get easier access to the Lizards that might be placed 
in a particular row or column. While scanning the input file for the configuration of the nursery the code also starts setting up the parameters for the 
heuristics like tree count and tree presence list column wise and free space which could be a potential place to keep the lizard.

The concept is simple we place a lizard at a particular location inside the nursery say (i,j), keeping in mind (For DFS and BFS)
1. There is no tree at that location
2. It doesn't get attacked by any lizard that is already there in the nursery (queens attack logic)
3. If a lizard cannot be kept in a particular column then the total number of trees in remaining columns(including the column where we failed to place the 
lizard) where the algorithm will try to place lizards, should have at least one tree. If no tree is present, then you can't place any lizards since placed 
lizards are attacking all the positions and tree which can provide a shelter from these attacks are not there.
4. Update the trackers regarding the location where this lizard was placed, if placed, so that whie checking for location cells to place a new lizard we can
get fast access with respect to column/row/diagonal where there might be conflicts with already placed lizards.
5. If a lizard conflict tracker tells that the current cell might result in conflict with already placed lizard in the nursery, check the tree tracker to see if 
there is any tree between this particular cell and the next closest placed lizard to this cell in diagonal/row/column so as to decide whether to place or not 
place the lizard.
6. After placeing a lizard update the free space count to check if the number of free spaces is greater than the remaining lizards.

The code follows the concept of the n queens algorithm with additional constraint of the trees. 

*/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class homework {
	//-------------------------------------------DFS variables-----------------------------------------//
	static Map<Integer, Integer> iLizard = new HashMap<>();

	static Map<Integer, Integer> jLizard = new HashMap<>();

	static Map<Integer, Boolean> leftDiagonal;

	static Map<Integer, Boolean> rightDiagonal;

	//-------------------------------------------End of DFS variables-----------------------------------------//	
	//Tree presence in future columns
	static List<Boolean> treePresence = new ArrayList<Boolean>();

	//Static variables for all recursions
	static int treeCount, lizardCapacity, size, nurseryCapacity;

	static char algo;

	static long startTime;

	public static void main(String... args) {
		
		try {
			//Variables Declaration
			int i=0, j=0, freeSpaceCount = 0;

			//Input File
			File file = new File("input.txt");
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line;

			//Get the type of algorithm
			line = bufferedReader.readLine();
			algo = line.charAt(0);

			//Get the matrix size
			line = bufferedReader.readLine();
			size = Integer.parseInt(line.trim());

			//Get the lizard capacity count
			line = bufferedReader.readLine();
			lizardCapacity = Integer.parseInt(line.trim());

			NurseryInstance nursery = new NurseryInstance();

			//Initialize the tree presence variable
			for(j=0; j<size; j++) {
				nursery.getTreeLocations().add(new ArrayList<Integer>());
				treePresence.add(false);
				nursery.getLizardPresence().add(0);
			}

			while (i<size && (line = bufferedReader.readLine()) != null) {
				//Initialize the matrix
				nursery.getNursery().add(new ArrayList<Integer>());

				for(j=0; j<size; j++) {
					//Add to the nursery
					nursery.getNursery().get(i).add(line.charAt(j)-48);
					nurseryCapacity++;

					//Count the number of trees
					if(line.charAt(j)!='0') {
						treeCount++;
						treePresence.set(j, true);
						nursery.getTreeLocations().get(j).add(i);
					}
				}

				//Increment the row counter
				i++;
			}
			fileReader.close();

			//Update the tree present values
			for(j=size-2; j>-1; j--) {
				treePresence.set(j, (treePresence.get(j+1)||treePresence.get(j)));
			}

			freeSpaceCount = nurseryCapacity-treeCount;

			//Apriori test
			if(freeSpaceCount<lizardCapacity || size<1 || (treeCount==0 && lizardCapacity>size)) {
				writeFile(null, false);
				return;
			} else if(lizardCapacity==0) {
				writeFile(nursery, true);
				return;
			}

			switch(algo) {
			case 'D' :
				//Default starting
				i=0;
				j=0;
	
				//Divide the DFS algo into 2 parts based on input - recursive and iterative
				if( lizardCapacity<30 ) {
					//Recursive approach relatively faster than iterative
					if(dfsExploreChildren(nursery, 0, freeSpaceCount, i, j)) {
						return;
					}
				} else {
					//Iterative approach incase of large lizards
					List<DFSInstance> dfsStack = new ArrayList<>();
					DFSInstance dfsInstance = new DFSInstance(0, freeSpaceCount, i, j, -1, -1, -1, -1);
					dfsStack.add(dfsInstance);
					if(dfsExploreChildren(nursery, dfsStack)) {
						return;
					}
				}
			
				break;
			case 'B' : 
				//Default starting
				i=0;
				j=0;
				
				List<NurseryInstance> queue = new ArrayList<>();
				
				queue.add(nursery);
				nursery.setExploreI(0);
				nursery.setExploreJ(0);
				nursery.setFeasibleFreeSpace(freeSpaceCount);
				if(exploreBFSQueue(queue)) {
					return;
				}
				
				break;
			case 'S' :
				if(sa(nursery)) {
					return;
				} else {
					//If sa fails to provide the output in less than 2 mins
					if(dfsExploreChildren(nursery, 0, freeSpaceCount, 0, 0)) {
						return;
					}
				}
				break;
			default : break;
			}

			writeFile(nursery, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*------------------------------------------------------------DFS Approaches---------------------------------------------------------------*/
	
	//-------------------------------------------------------1. Recursive Approach--------------------------------------------------------------
	//Function responsible for exploring the possible nodes from given node for dfs
	static boolean dfsExploreChildren(NurseryInstance nursery, int placedLizards, int feasibleFreeSpace, int exploreI, int exploreJ) {
		//Find a valid location from the given exploration nodes inclusive
		while(exploreJ<size) {
			//Check out the new (i,j) position
			while(exploreI<size && nursery.getNursery().get(exploreI).get(exploreJ)!=0) {
				//Go to next row
				exploreI++;
			}

			//No suitable place in this column
			if(exploreI>=size) {
				//If no lizard was placed in that column and there is no tree for further matrix including that column
				if(nursery.getLizardPresence().get(exploreJ)==0 && !treePresence.get(exploreJ) && lizardCapacity-placedLizards>size-exploreJ) {
					break;
				}

				//Hop to next column
				exploreJ++;
				exploreI=0;
			} else {
				//Found a valid lizard location and free spaces can accommodate those many lizards
				int placeI = exploreI, placeJ = exploreJ, nodeFeasibleFreeSpace=feasibleFreeSpace;

				//Put the lizard
				nursery.getNursery().get(placeI).set(placeJ, 1);

				//Increase the lizard placed count
				placedLizards++;

				//Capacity reached
				if(placedLizards==lizardCapacity) {
					writeFile(nursery, true);
					return true;
				}

				//Remove the space consumed by lizard
				nodeFeasibleFreeSpace--;

				//Update Lizard presence variable for that column
				nursery.getLizardPresence().set(placeJ, nursery.getLizardPresence().get(placeJ)+1);

				//---------------------------------------Calculation of unavailable spaces-------------------------------------//
				//Find the max distance between the current position and the sides
				int maxSide = placeI;
				if(maxSide<size-1-placeI) {
					maxSide = size-1-placeI;
				}
				if(maxSide<placeJ) {
					maxSide = placeJ;
				}
				if(maxSide<size-1-placeJ) {
					maxSide = size-1-placeJ;
				}

				//Decrease the free spaces
				nodeFeasibleFreeSpace = updateFreeSpaces(nursery, maxSide, placeI, placeJ, nodeFeasibleFreeSpace, 3);
				//-------------------------------------------------------------------------------------------------------------//

				//Check whether the updated free spaces are considerable
				if(nodeFeasibleFreeSpace>=lizardCapacity-placedLizards) {

					//-----------------------------------Initiate a new node search which will be next tree in that column + 1-----------------------------//
					exploreI+=2;
					if( dfsExploreChildren(nursery, placedLizards, nodeFeasibleFreeSpace, exploreI, exploreJ) ) {
						return true;
					}
				}

				//Remove the lizard and associated counters
				nursery.getNursery().get(placeI).set(placeJ, 0);
				placedLizards--;
				nursery.getLizardPresence().set(placeJ, nursery.getLizardPresence().get(placeJ)-1);

				//Considered space reduced as it is no longer for consideration
				feasibleFreeSpace--;

				//Free the spaces blocked by this lizard in the matrix
				updateFreeSpaces(nursery, maxSide, placeI, placeJ, 0, -3);

				//Destabalize the value of exploreI
				exploreI = placeI+1;
				//Restore the value of exploreJ
				exploreJ = placeJ;

				if(feasibleFreeSpace<lizardCapacity-placedLizards) {
					exploreJ = size;
				}
			}
		}

		//Couldn't place all the lizards
		return false;
	}

	//---------------------------------------------------------------2. Iterative Approach----------------------------------------------------------------
	//Function responsible for exploring the possible nodes from given node for dfs
	static boolean dfsExploreChildren(NurseryInstance nursery, List<DFSInstance> dfsStack) {
		while(dfsStack.size()>0) {
			//Refer out the element from the stack
			DFSInstance dfsInstance = dfsStack.get( dfsStack.size()-1 );

			//Initialize the trees removed list
			Map<Integer,List<Integer>> mainRemovedTreesInstance = dfsInstance.getMainRemovedTreesInstance();
			int exploreJ = dfsInstance.getExploreJ();
			int exploreI = dfsInstance.getExploreI();
			int feasibleFreeSpace = dfsInstance.getFeasibleFreeSpace();
			int placedLizards = dfsInstance.getPlacedLizards();

			mainRemovedTreesInstance.put(exploreJ, new ArrayList<Integer>());


			//End point reached or lack of free space
			if(exploreJ>=size || feasibleFreeSpace<lizardCapacity-placedLizards) {
				if(dfsInstance.getLastI()==-1) {
					//Stop the algorithm
					return false;
				}

				//Remove the lizard and associated counters
				nursery.getNursery().get(dfsInstance.getLastI()).set(dfsInstance.getLastJ(), 0);

				//Update Lizard presence variable for that column
				nursery.getLizardPresence().set(dfsInstance.getLastJ(), nursery.getLizardPresence().get(dfsInstance.getLastJ())-1);

				//Free the spaces blocked by this lizard in the matrix
				updateFreeSpaces(nursery, dfsInstance.getMaxSide(), dfsInstance.getLastI(), dfsInstance.getLastJ(), 0, -3);

				//Add the Tree that were sacrificed to place the lizard at placeI, placeJ
				for(Integer key : mainRemovedTreesInstance.keySet()) {
					for(int i = mainRemovedTreesInstance.get(key).size()-1 ; i>-1; i--) {
						//Re-insert the trees in the reverse order
						nursery.getTreeLocations().get(key).add(0, mainRemovedTreesInstance.get(key).get(i));
					}
				}				

				//Restore the removed tree to reach this node, if any
				if(dfsInstance.getRemovedTree()>-1) {
					//Fetch all the tree locations for that column
					List<Integer> jTrees = nursery.getTreeLocations().get(dfsInstance.getLastJ());
					jTrees.add(0, dfsInstance.getRemovedTree());
				}

				//Pop out the element from the stack
				dfsStack.remove( dfsStack.size()-1 );
			} else {
				/*Find a valid location from the given exploration nodes inclusive*/

				//Check out the new (i,j) position
				while(exploreI<size && nursery.getNursery().get(exploreI).get(exploreJ)!=0) {
					//If the location is tree
					if(nursery.getNursery().get(exploreI).get(exploreJ)==2) {
						//For restoring the dfs back to the state it initiated from
						mainRemovedTreesInstance.get(exploreJ).add(exploreI);

						//If you encounter a tree remove it as it is beyond feasible matrix range
						nursery.getTreeLocations().get(exploreJ).remove(0);
					}
					//Go to next row
					exploreI++;
				}

				//No suitable place in this column
				if(exploreI>=size) {
					//If no lizard was placed in that column and there is no tree for further matrix including that column
					if(nursery.getLizardPresence().get(exploreJ)==0 && !treePresence.get(exploreJ) && lizardCapacity-placedLizards>size-exploreJ-1) {
						break;
					}

					//Hop to next column
					exploreJ++;
					exploreI=0;

					//Update the coordinates for the next check
					dfsInstance.setExploreI(exploreI);
					dfsInstance.setExploreJ(exploreJ);

					//Remove the unnecessary key, if required
					if( mainRemovedTreesInstance.get(exploreJ-1).size()==0 ) {
						mainRemovedTreesInstance.remove(exploreJ-1);
					}
					//Prepare for the next iteration by initializing the array to store the tree that might be removed in them
					if(exploreJ<size) {
						mainRemovedTreesInstance.put(exploreJ, new ArrayList<Integer>());
					}
				} else {
					//Found a valid lizard location and free spaces can accommodate those many lizards
					int placeI = exploreI, placeJ = exploreJ, nodeFeasibleFreeSpace=feasibleFreeSpace-1;

					//Already subtracted for this node as in future also we subtract for this position
					dfsInstance.setFeasibleFreeSpace(nodeFeasibleFreeSpace);

					//---------------------------------------Get the things ready for the next iteration--------------------------------
					//Put the lizard
					nursery.getNursery().get(placeI).set(placeJ, 1);

					//Increase the lizard placed count
					placedLizards++;

					//Capacity reached
					if(placedLizards==lizardCapacity) {
						writeFile(nursery, true);
						return true;
					}

					//Update Lizard presence variable for that column
					nursery.getLizardPresence().set(placeJ, nursery.getLizardPresence().get(placeJ)+1);

					//---------------------------------------Calculation of unavailable spaces-------------------------------------//
					//Find the max distance between the current position and the sides
					int maxSide = placeI;
					if(maxSide<size-1-placeI) {
						maxSide = size-1-placeI;
					}
					if(maxSide<placeJ) {
						maxSide = placeJ;
					}
					if(maxSide<size-1-placeJ) {
						maxSide = size-1-placeJ;
					}

					//Decrease the free spaces
					nodeFeasibleFreeSpace = updateFreeSpaces(nursery, maxSide, placeI, placeJ, nodeFeasibleFreeSpace, 3);
					//-------------------------------------------------------------------------------------------------------------//

					//Check whether the updated free spaces are considerable
					if(nodeFeasibleFreeSpace>=lizardCapacity-placedLizards) {
						//Fetch all the tree locations for that column
						List<Integer> jTrees = nursery.getTreeLocations().get(placeJ);

						//-----------------------------------Initiate a new node search which will be next tree in that column + 1-----------------------------//
						//Store the removed tree
						int removedTree = -1;

						if(jTrees.size()>0) {
							removedTree = jTrees.get(0);
							exploreI = removedTree+1;
							jTrees.remove(0);
						} else {
							exploreJ++;
							exploreI = 0;
						}

						//Check the limits
						if(exploreJ<size) {
							DFSInstance possibleSolution = new DFSInstance(placedLizards, nodeFeasibleFreeSpace, exploreI, exploreJ, placeI, placeJ, removedTree, maxSide);
							dfsStack.add(possibleSolution);
						}
					}

					//Update the parent
					dfsInstance.setExploreI( dfsInstance.getExploreI()+1 );
				}
			}
		}	

		//Couldn't place all the lizards
		return false;
	}

	//Function that updates the state space by removeing invalid free spaces due to insertion of a lizard and returns the remaining free spaces
	static Integer updateFreeSpaces(NurseryInstance nursery, int maxSide, int exploreI, int exploreJ, int feasibleFreeSpaceCount, int operation) {
		//One tree flag for each direction
		boolean tree[]= {false, false, false, false, false, false, false, false};

		//Update the free spaces
		for(int i=1; i<=maxSide; i++) {
			//Upwards
			if(exploreI-i>-1 && !tree[0]) {
				int loc = nursery.getNursery().get(exploreI-i).get(exploreJ);
				if(loc==2) {
					tree[0]=true;
				} else {
					nursery.getNursery().get(exploreI-i).set(exploreJ, loc + operation);
				}
			}

			//topRight Diagonal
			if(exploreI-i>-1 && exploreJ+i<size && !tree[1]) {
				int loc = nursery.getNursery().get(exploreI-i).get(exploreJ+i);
				if(loc==2) {
					tree[1]=true;
				} else {
					nursery.getNursery().get(exploreI-i).set(exploreJ+i, loc + operation);
					if(loc==0) {
						feasibleFreeSpaceCount--;
					}
				}
			}

			//Right
			if(exploreJ+i<size && !tree[2]) {
				int loc = nursery.getNursery().get(exploreI).get(exploreJ+i);
				if(loc==2) {
					tree[2]=true;
				} else {
					nursery.getNursery().get(exploreI).set(exploreJ+i, loc + operation);
					if(loc==0) {
						feasibleFreeSpaceCount--;
					}
				}
			}

			//Right Bottom
			if(exploreJ+i<size && exploreI+i<size && !tree[3]) {
				int loc = nursery.getNursery().get(exploreI+i).get(exploreJ+i); 
				if(loc==2) {
					tree[3]=true;
				} else {
					nursery.getNursery().get(exploreI+i).set(exploreJ+i, loc + operation);
					if(loc==0) {
						feasibleFreeSpaceCount--;
					}
				}
			}

			//Downwards
			if(exploreI+i<size && !tree[4]) {
				int loc = nursery.getNursery().get(exploreI+i).get(exploreJ);
				if(loc==2) {
					tree[4]=true;
				} else {
					nursery.getNursery().get(exploreI+i).set(exploreJ, loc + operation);
					if(loc==0) {
						feasibleFreeSpaceCount--;
					}
				}
			}

			//Left Bottom
			if(exploreI+i<size && exploreJ-i>-1 && !tree[5]) {
				int loc = nursery.getNursery().get(exploreI+i).get(exploreJ-i); 
				if(loc==2) {
					tree[5]=true;
				} else {
					nursery.getNursery().get(exploreI+i).set(exploreJ-i, loc + operation);
				}
			}

			//Left
			if(exploreJ-i>-1 && !tree[6]) {
				int loc = nursery.getNursery().get(exploreI).get(exploreJ-i);
				if(loc==2) {
					tree[6]=true;
				} else {
					nursery.getNursery().get(exploreI).set(exploreJ-i, loc + operation);
				}
			}

			//Left top Diagonal
			if(exploreI-i>-1 && exploreJ-i>-1 && !tree[7]) {
				int loc = nursery.getNursery().get(exploreI-i).get(exploreJ-i);
				if(loc==2) {
					tree[7]=true;
				} else {
					nursery.getNursery().get(exploreI-i).set(exploreJ-i, loc + operation);
				}
			}
		}

		//Return the number of free Spaces and feasible free space left
		return feasibleFreeSpaceCount;
	}

	static boolean exploreBFSQueue(List<NurseryInstance> queue){
		while(queue.size()>0) {
			//Fetch an instance from queue
			NurseryInstance localInstance = queue.get(0);
			queue.remove(0);
			if(bfsExploreChildren(localInstance, queue)) {
				return true;
			}
		}
		return false;
	}

	static boolean bfsExploreChildren(NurseryInstance nursery, List<NurseryInstance> queue) {
		//Find a valid location from the given exploration nodes inclusive
		int i = nursery.getExploreI(), j = nursery.getExploreJ();

		while(j<size) {
			//Check out the new (i,j) position
			while(i<size && nursery.getNursery().get(i).get(j)!=0) {
				//If the location is tree
				if(nursery.getNursery().get(i).get(j)==2) {
					//If you encounter a tree remove it as it is beyond feasible matrix range
					nursery.getTreeLocations().get(j).remove(0);
				}
				//Go to next row
				i++;
			}

			//No suitable place in this column
			if(i>=size) {
				//If no lizard was placed in that column and there is no tree for further matrix including that column
				if(nursery.getLizardPresence().get(j)==0 && !treePresence.get(j) && lizardCapacity-nursery.getPlacedLizards()>size-nursery.getExploreJ()-1) {
					//No solution
					return false;
				}

				//Hop to next column
				j++;
				i=0;
			} else {
				//Found a valid lizard location and free spaces can accommodate those many lizards
				nursery.setFeasibleFreeSpace( nursery.getFeasibleFreeSpace()-1);

				NurseryInstance possibleInstance = new NurseryInstance(nursery, i, j);

				//While processing that node check whether that is the solution
				if(possibleInstance.getPlacedLizards()==lizardCapacity) {
					writeFile(possibleInstance, true);
					return true;
				}

				//Ignore this place and move on to find next place to put the lizard
				if(possibleInstance.getExploreI()>-1) {
					queue.add(possibleInstance);
				}
				//Move forward
				i++;

				if(nursery.getFeasibleFreeSpace()<lizardCapacity-nursery.getPlacedLizards()) {
					j = size;
				}
			}
		}

		//Couldn't place all the lizards
		return false;
	}

	//----------------------------------------------------------------------------------------------SA-------------------------------------------------------------------------------//

	static boolean sa(NurseryInstance nursery) {
		int conflicts, newConflicts, delta;
		long timeDifference=0;
		double temperature = size*lizardCapacity, coolingFactor = 0.05, stabalizationFactor = 1.005, stabalizer = temperature;

		//Normal list of lizards
		List<Map<Character, Integer>> lizardList = new ArrayList<Map<Character, Integer>>();

		Map<Character, List<Integer>> possibleSwap = new HashMap<>();

		//Stores the location of the lizard wrt i
		Map<Integer, Map <Integer,Boolean>> iLizard = new HashMap<>();

		startTime = System.currentTimeMillis();

		//Initialize a initial solution
		conflicts = generateRandomSolution(nursery, lizardList, iLizard);

		//Real time check to be less than 2 mins apart from temperature check
		while(temperature>0.0 && timeDifference<120000 ) {
			for(int i=0; i<stabalizer; i++) {
				//If the temperature has reached zero and we have to see if we managed to get the solution
				if(conflicts==0) {
					//Prepare the nursery using the lizard list
					for(int j=0; j<lizardCapacity; j++) {
						nursery.getNursery().get(lizardList.get(j).get('x')).set(lizardList.get(j).get('y'), 1);
					}

					writeFile(nursery, true);
					return true;
				}

				//Create a new instance for neighbour
				NurseryInstance localInstance = new NurseryInstance(nursery);

				//The modified Lizards for keeping in track
				possibleSwap.put('l', new ArrayList<Integer>());
				//Coordinates that might be added
				possibleSwap.put('a', new ArrayList<Integer>());
				//Coordinates that might be removed
				possibleSwap.put('r', new ArrayList<Integer>());

				newConflicts = findNeighbour(localInstance, lizardList, possibleSwap, conflicts, iLizard);

				delta = newConflicts - conflicts;

				if(probability_function(temperature, delta)) {
					nursery = localInstance;
					conflicts = newConflicts;

					//Update the Lizard List
					lizardList.get( possibleSwap.get('l').get(0) ).put( 'x', possibleSwap.get('a').get(0) );
					lizardList.get( possibleSwap.get('l').get(0) ).put( 'y', possibleSwap.get('a').get(1) );

					//---------------------Update the iLizard---------------------
					iLizard.get( possibleSwap.get('r').get(0) ).remove( possibleSwap.get('r').get(1) );

					if( iLizard.containsKey(possibleSwap.get('a').get(0)) ){
						iLizard.put(possibleSwap.get('a').get(0), new HashMap<>());
						iLizard.get(possibleSwap.get('a').get(0)).put(possibleSwap.get('a').get(1), true);
					}

				}
			}

			//Decrease the temperature linearly
			temperature = temperature - coolingFactor;

			//Decrease the stabalizer
			stabalizer = stabalizer * stabalizationFactor;
			
			//Time Difference
			timeDifference = System.currentTimeMillis() - startTime;
		}

		if(conflicts==0) {
			writeFile(nursery, true);
			return true;
		}

		return false;
	}

	//Initialize a random solution
	static int generateRandomSolution(NurseryInstance nursery, List<Map<Character, Integer>> lizardList, Map<Integer, Map <Integer,Boolean>> iLizard) {
		boolean conflictingCase = true;
		int conflicts = 0;

		//Loop till we place all the lizards
		for(int i=0;i<lizardCapacity; i++) {
			//To check whether the randomized location is valid
			int x,y;

			//Reinitialize the conflict case variable
			conflictingCase = true;

			//Add the ith Lizardcoordinates in terms of x and y as key
			lizardList.add(new HashMap<Character, Integer>());

			//If it turns out to be same location as other lizards or has trees
			while(conflictingCase) {
				//Pick up a random spot
				x = (int) (Math.random() * size);
				y = (int) (Math.random() * size);

				//No trees and no lizards on that place
				if( nursery.getNursery().get(x).get(y)!=2 && !(iLizard.containsKey(x) && iLizard.get(x).containsKey(y)) ) {
					if(!(iLizard.containsKey(x))) {
						iLizard.put(x, new HashMap<>());
					}

					//Store the Lizard
					iLizard.get(x).put(y, true);

					//------------------------------Update the unavailable free spaces--------------------------------
					//Calculate the max width
					int maxSide = x;
					if(maxSide<size-1-x) {
						maxSide = size-1-x;
					}
					if(maxSide<y) {
						maxSide = y;
					}
					if(maxSide<size-1-y) {
						maxSide = size-1-y;
					}

					updateFreeSpaces(nursery, maxSide, x, y, 0, 3);

					//Update the number of conflicts
					conflicts+= 2*(nursery.getNursery().get(x).get(y)/3);

					//Update the space where the Lizard is being kept
					nursery.getNursery().get(x).set(y, nursery.getNursery().get(x).get(y) + 3);

					//Exit loop condition
					conflictingCase = false;

					//Update the lizard list
					lizardList.get(i).put('x', x);
					lizardList.get(i).put('y', y);
				}
			}
		}

		return conflicts;
	}

	//Find the next probable neighbour
	static int findNeighbour(NurseryInstance localInstance, List<Map<Character, Integer>> lizardList, Map<Character,List<Integer>> possibleSwap, int conflicts, Map<Integer, Map <Integer,Boolean>> iLizard) {
		int x, y, randomLizard;
		boolean conflictingCase = true;

		//Pick up any lizard at random
		randomLizard = (int) (Math.random()*lizardCapacity);

		x = lizardList.get(randomLizard).get('x');
		y = lizardList.get(randomLizard).get('y');

		//Update the space where the Lizard is being kept
		localInstance.getNursery().get(x).set(y, localInstance.getNursery().get(x).get(y) - 3);

		//Reduce the number of conflicts
		conflicts = conflicts - (localInstance.getNursery().get(x).get(y)/3)*2;

		//Calculate the max width
		int maxSide = x;
		if(maxSide<size-1-x) {
			maxSide = size-1-x;
		}
		if(maxSide<y) {
			maxSide = y;
		}
		if(maxSide<size-1-y) {
			maxSide = size-1-y;
		}

		//Clear out rest of the matrix
		updateFreeSpaces(localInstance, maxSide, x, y, 0, -3);

		//Store the location of the old x and y which are to be removed from the iLizard list
		possibleSwap.get('r').add(x);
		possibleSwap.get('r').add(y);

		//Place to be updated in the list
		possibleSwap.get('l').add(randomLizard);

		//-------------------------------------------------------------------Possible places to place the lizard-------------------------------------------------------

		//If it turns out to be same location as other lizards or has trees
		while(conflictingCase) {
			//Pick up a random spot
			x = (int) (Math.random() * size);
			y = (int) (Math.random() * size);

			if( localInstance.getNursery().get(x).get(y)!=2 && !(iLizard.containsKey(x) && iLizard.get(x).containsKey(y)) ) {
				if(!(iLizard.containsKey(x))) {
					iLizard.put(x, new HashMap<>());
				}

				//Calculate the trouble going to be caused by that lizard
				conflicts += 2*(localInstance.getNursery().get(x).get(y)/3);

				//------------------------------Update the unavailable free spaces--------------------------------
				//Calculate the max width
				maxSide = x;
				if(maxSide<size-1-x) {
					maxSide = size-1-x;
				}
				if(maxSide<y) {
					maxSide = y;
				}
				if(maxSide<size-1-y) {
					maxSide = size-1-y;
				}

				updateFreeSpaces(localInstance, maxSide, x, y, 0, 3);

				//Update the space where the Lizard is being kept
				localInstance.getNursery().get(x).set(y, localInstance.getNursery().get(x).get(y) + 3);

				//Store the location of the old x and y which are to be removed from the iLizard list
				possibleSwap.get('a').add(x);
				possibleSwap.get('a').add(y);

				//Exit loop condition
				conflictingCase = false;
			}
		}

		//Send the updated value of conflicts
		return conflicts;
	}

	//Calculate the probability
	static boolean probability_function(double temperature, int delta) {
		if(delta<0) {
			return true;
		}

		double c = Math.exp(((double) -delta) / temperature);
		double r = Math.random();

		if(r<c) {
			return true;
		}

		return false;
	}

	static void writeFile(NurseryInstance nursery, boolean result) {
		PrintWriter writer;
		try {
			writer = new PrintWriter("output.txt", "UTF-8");
			StringBuffer sb = new StringBuffer();
			if(result) {
				writer.println("OK");

				for(int i=0;i<size;i++) {
					for(int j=0; j<size; j++) {
						if(nursery.getNursery().get(i).get(j)>2) {
							sb.append(0);
						} else {
							sb.append(nursery.getNursery().get(i).get(j));
						}
					}
					writer.println(sb);
					//Clear the string buffer
					sb.delete(0, sb.length());
				}
			} else {
				writer.println("FAIL");
			}

			writer.close();
		} catch (Exception e) {
			//Auto-generated catch block
			System.out.println("Failed in writing file");
			e.printStackTrace();
		}
	}
}

/*Class containing the nursary instance*/
class NurseryInstance{
	private List<List<Integer>> nursery = new ArrayList<List<Integer>>();

	//Lizard presence in columns
	private List<Integer> lizardPresence = new ArrayList<Integer>();

	//Tree Locations
	private List<List<Integer>> treeLocations = new ArrayList<List<Integer>>();

	private int exploreI;

	private int exploreJ;

	private int placedLizards;

	private int feasibleFreeSpace;

	public int getPlacedLizards() {
		return placedLizards;
	}

	public void setPlacedLizards(int placedLizards) {
		this.placedLizards = placedLizards;
	}

	public int getFeasibleFreeSpace() {
		return feasibleFreeSpace;
	}

	public void setFeasibleFreeSpace(int feasibleFreeSpace) {
		this.feasibleFreeSpace = feasibleFreeSpace;
	}

	public List<List<Integer>> getNursery() {
		return nursery;
	}

	public void setNursery(List<List<Integer>> nursery) {
		this.nursery = nursery;
	}

	public List<Integer> getLizardPresence() {
		return lizardPresence;
	}

	public void setLizardPresence(List<Integer> lizardPresence) {
		this.lizardPresence = lizardPresence;
	}

	public List<List<Integer>> getTreeLocations() {
		return treeLocations;
	}

	public void setTreeLocations(List<List<Integer>> treeLocations) {
		this.treeLocations = treeLocations;
	}

	public int getExploreI() {
		return exploreI;
	}

	public void setExploreI(int exploreI) {
		this.exploreI = exploreI;
	}

	public int getExploreJ() {
		return exploreJ;
	}

	public void setExploreJ(int exploreJ) {
		this.exploreJ = exploreJ;
	}

	public NurseryInstance() {
		//Auto-generated constructor stub
	}

	public NurseryInstance(NurseryInstance nurseryOriginal, int exploreI, int exploreJ) {
		// Copy the full Nursery, treeLocations and lizardsPresence
		for(int i=0; i< nurseryOriginal.getNursery().size(); i++) {
			nursery.add(new ArrayList<Integer>());
			treeLocations.add(new ArrayList<Integer>());

			//Add in the order of columns
			lizardPresence.add( nurseryOriginal.getLizardPresence().get(i));
			for(int j=0; j<nurseryOriginal.getTreeLocations().get(i).size(); j++) {
				treeLocations.get(i).add( nurseryOriginal.getTreeLocations().get(i).get(j));
			}

			for(int j=0; j<nurseryOriginal.getNursery().size(); j++) {
				nursery.get(i).add( nurseryOriginal.getNursery().get(i).get(j) );
			}
		}

		//Lizard placed
		nursery.get(exploreI).set(exploreJ, 1);

		//Update the count
		this.placedLizards = nurseryOriginal.getPlacedLizards()+1;

		//Update with feasible space cosidering lizard placed
		this.feasibleFreeSpace = nurseryOriginal.getFeasibleFreeSpace();

		//Update Lizard presence variable for that column
		lizardPresence.set(exploreJ, lizardPresence.get(exploreJ)+1);

		//---------------------------------------Calculation of unavailable spaces-------------------------------------//
		//Find the max distance between the current position and the sides
		int maxSide = exploreI;
		if(maxSide<homework.size-1-exploreI) {
			maxSide = homework.size-1-exploreI;
		}
		if(maxSide<exploreJ) {
			maxSide = exploreJ;
		}
		if(maxSide<homework.size-1-exploreJ) {
			maxSide = homework.size-1-exploreJ;
		}

		//Decrease the free spaces
		feasibleFreeSpace = homework.updateFreeSpaces(this, maxSide, exploreI, exploreJ, feasibleFreeSpace, 3);
		//-------------------------------------------------------------------------------------------------------------//


		//Check whether the updated free spaces are considerable
		if(feasibleFreeSpace>=homework.lizardCapacity-placedLizards) {
			//Fetch all the tree locations for that column
			List<Integer> jTrees = treeLocations.get(exploreJ);

			//-----------------------------------Initiate a new node search which will be next tree in that column + 1-----------------------------//

			//Update the i and j with values for next search starting
			if(jTrees.size()>0) {
				this.exploreI = jTrees.get(0)+1;
				this.exploreJ = exploreJ;
				jTrees.remove(0);
			} else {
				this.exploreJ = exploreJ+1;
				this.exploreI = 0;
			}

			//Check the limits
			if(exploreJ<homework.size) {
				return;
			}
		}

		//If fails check
		this.exploreI = -1;
	}

	//Normal constructor for creating a matrix copy
	public NurseryInstance(NurseryInstance nurseryOriginal) {
		// Copy the full Nursery, treeLocations and lizardsPresence
		for(int i=0; i< nurseryOriginal.getNursery().size(); i++) {
			nursery.add(new ArrayList<Integer>());

			for(int j=0; j<nurseryOriginal.getNursery().size(); j++) {
				nursery.get(i).add( nurseryOriginal.getNursery().get(i).get(j) );
			}
		}
	}
}

class DFSInstance {
	private Map<Integer,List<Integer>> mainRemovedTreesInstance;

	private int exploreI;

	private int exploreJ;

	private int placedLizards;

	private int feasibleFreeSpace;

	private int removedTree;

	private int lastI;

	private int lastJ;

	private int maxSide;

	public DFSInstance(int placedLizards, int feasibleFreeSpace, int exploreI, int exploreJ, int lastI, int lastJ, int removedTree, int maxSide) {
		this.placedLizards = placedLizards;
		this.feasibleFreeSpace = feasibleFreeSpace;
		this.exploreI = exploreI;
		this.exploreJ = exploreJ;
		this.mainRemovedTreesInstance = new HashMap<Integer, List<Integer>>();
		this.removedTree = removedTree;
		this.lastI = lastI;
		this.lastJ = lastJ;
		this.maxSide = maxSide;
	}

	public int getMaxSide() {
		return maxSide;
	}

	public void setMaxSide(int maxSide) {
		this.maxSide = maxSide;
	}

	public int getLastI() {
		return lastI;
	}

	public void setLastI(int lastI) {
		this.lastI = lastI;
	}

	public int getLastJ() {
		return lastJ;
	}

	public void setLastJ(int lastJ) {
		this.lastJ = lastJ;
	}

	public Map<Integer, List<Integer>> getMainRemovedTreesInstance() {
		return mainRemovedTreesInstance;
	}

	public void setMainRemovedTreesInstance(Map<Integer, List<Integer>> mainRemovedTreesInstance) {
		this.mainRemovedTreesInstance = mainRemovedTreesInstance;
	}

	public int getExploreI() {
		return exploreI;
	}

	public void setExploreI(int exploreI) {
		this.exploreI = exploreI;
	}

	public int getExploreJ() {
		return exploreJ;
	}

	public void setExploreJ(int exploreJ) {
		this.exploreJ = exploreJ;
	}

	public int getPlacedLizards() {
		return placedLizards;
	}

	public void setPlacedLizards(int placedLizards) {
		this.placedLizards = placedLizards;
	}

	public int getFeasibleFreeSpace() {
		return feasibleFreeSpace;
	}

	public void setFeasibleFreeSpace(int feasibleFreeSpace) {
		this.feasibleFreeSpace = feasibleFreeSpace;
	}

	public int getRemovedTree() {
		return removedTree;
	}

	public void setRemovedTree(int removedTree) {
		this.removedTree = removedTree;
	}

}
