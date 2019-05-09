package it.unibo.ai.didattica.competition.tablut.client.lori;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import aima.core.search.adversarial.Game;
import it.unibo.ai.didattica.competition.tablut.domain.*;
import it.unibo.ai.didattica.competition.tablut.domain.State.Pawn;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;


/* Questa classe estende la classe GameAshtonTablut del progetto del prof.
 * Inoltre implementa l'interfaccia Game della libreria aima e i rispettivi metodi:
 */

/**
 * @author L.Piazza
 *
 */
public class MyGame extends GameAshtonTablut implements Game<State, Action, Turn> {
	
	//Costruttori che semplicemente richiamano i costruttori della classe GameAshtonTablut
	public MyGame(int repeated_moves_allowed, int cache_size, String logs_folder, String whiteName, String blackName) {
		super(repeated_moves_allowed, cache_size, logs_folder, whiteName, blackName);
	}
	
	public MyGame(State state, int repeated_moves_allowed, int cache_size, String logs_folder,
			String whiteName, String blackName) {
		super(state, repeated_moves_allowed, cache_size, logs_folder, whiteName, blackName);

	}
	
	//Costruttore in cui passo uno stato, un GameAshton Tablut, il whiteName e il blackName
	public MyGame(State state, GameAshtonTablut game, String whiteName, String blackName) {
		super(state, game.getRepeated_moves_allowed(), game.getCache_size(), game.getGameLog().getName(), whiteName, blackName);

	}
	
	/* 
	 * Questa funzione a differenza della checkMove gi� fornita salta alcuni controlli inutili per come vengono fornite le mosse da valutare (es: impossibile valutare una mossa in diagonale quindi non controllo).
	 * Un'altra differenza � che non muove la pedina nel caso la mossa sia possibile.
	 * Resituisce 0 se la mossa � possibile, 1 altrimenti.
	 * Viene chiamata in seguito all'interno di getActions().
	 */
	public int myCheckMove(int columnFrom, int columnTo, int rowFrom, int rowTo, int ctrl, State state){

		if (columnFrom > state.getBoard().length - 1 || rowFrom > state.getBoard().length - 1
				|| rowTo > state.getBoard().length - 1 || columnTo > state.getBoard().length - 1 
				||columnFrom < 0 || rowFrom < 0 || rowTo < 0 || columnTo < 0) 
			ctrl=1;

		// controllo che non vada sul trono
		if (state.getPawn(rowTo, columnTo).equalsPawn(State.Pawn.THRONE.toString()))
			ctrl=1;

		// controllo la casella di arrivo
		if (!state.getPawn(rowTo, columnTo).equalsPawn(State.Pawn.EMPTY.toString()))
			ctrl=1;
		
		if (this.getCitadels().contains(state.getBox(rowTo, columnTo))
				&& !this.getCitadels().contains(state.getBox(rowFrom, columnFrom)))
			ctrl=1;
		
		if (this.getCitadels().contains(state.getBox(rowTo, columnTo))
				&& this.getCitadels().contains(state.getBox(rowFrom, columnFrom))) {
			if (rowFrom == rowTo) {
				if (columnFrom - columnTo > 5 || columnFrom - columnTo < -5)
					ctrl=1;
			} else {
				if (rowFrom - rowTo > 5 || rowFrom - rowTo < -5) 
					ctrl=1;
			}
		}

		// controllo se cerco di stare fermo
		if (rowFrom == rowTo && columnFrom == columnTo) 
			ctrl=1;

		// controllo se sto muovendo una pedina giusta
		if (state.getTurn().equalsTurn(State.Turn.WHITE.toString())) {
			if (!state.getPawn(rowFrom, columnFrom).equalsPawn("W")
					&& !state.getPawn(rowFrom, columnFrom).equalsPawn("K")) 
				ctrl=1;
		}
		
		if (state.getTurn().equalsTurn(State.Turn.BLACK.toString())) {
			if (!state.getPawn(rowFrom, columnFrom).equalsPawn("B"))
				ctrl=1;
		}

		// controllo di non scavalcare pedine
		if (rowFrom == rowTo) {
			if (columnFrom > columnTo) {
				for (int i = columnTo; i < columnFrom; i++) {
					if (!state.getPawn(rowFrom, i).equalsPawn(State.Pawn.EMPTY.toString())) 
						ctrl=1;
					if (this.getCitadels().contains(state.getBox(rowFrom, i))
							&& !this.getCitadels().contains(state.getBox(rowFrom, columnFrom))) 
						ctrl=1;
				}
			} else {
				for (int i = columnFrom + 1; i <= columnTo; i++) {
					if (!state.getPawn(rowFrom, i).equalsPawn(State.Pawn.EMPTY.toString()))
						ctrl=1;
					if (this.getCitadels().contains(state.getBox(rowFrom, i))
							&& !this.getCitadels().contains(state.getBox(rowFrom, columnFrom)))
						ctrl=1;
				}
			}
		} else {
			if (rowFrom > rowTo) {
				for (int i = rowTo; i < rowFrom; i++) {
					if (!state.getPawn(i, columnFrom).equalsPawn(State.Pawn.EMPTY.toString()))
						ctrl=1;
					if (this.getCitadels().contains(state.getBox(i, columnFrom))
							&& !this.getCitadels().contains(state.getBox(rowFrom, columnFrom)))
						ctrl=1;
				}
			} else {
				for (int i = rowFrom + 1; i <= rowTo; i++) {
					if (!state.getPawn(i, columnFrom).equalsPawn(State.Pawn.EMPTY.toString())) 
						ctrl=1;
					if (this.getCitadels().contains(state.getBox(i, columnFrom))
							&& !this.getCitadels().contains(state.getBox(rowFrom, columnFrom))) {
						ctrl=1;
					}
				}
			}
		}
		return ctrl;
	}

	
	//Da qui in poi implemento i metodi richiesti dall'interfaccia aima.core.search.adversarial.Game:
	
	/*	
	 * getActions(State state):
	 * Restituisce una lista di tutte le azioni possibili del giocatore a cui tocca muovere.
	 * Capisce a chi tocca muovere controllando il turno insito nello stato che gli viene passato).
	 * Se il turno dello stato passato � WHITEWIN, BALCKWIN o DRAW restituisce la lista di azioni vuota.
	 * 
	 * Codice by A.Fuschino (alcune modifiche by L.Piazza)
	 */
	
	@Override
	public List<Action> getActions(State state) {
		return myGetActions(state);	

	}
	
	private List<Action> myGetActions(State state){
		List<int[]> white = new ArrayList<int[]>(); //tengo traccia della posizione nello stato dei bianchi
		List<int[]> black = new ArrayList<int[]>(); //uguale per i neri

		int[] buf; //mi indica la posizione ex."z6" 

		for (int i = 0; i < state.getBoard().length; i++) {
			for (int j = 0; j < state.getBoard().length; j++) {
				if (state.getPawn(i, j).equalsPawn("W") 
						|| state.getPawn(i, j).equalsPawn("K")) {
					buf = new int[2];
					buf[0] = i;
					//System.out.println( "riga: " + buf[0] + " ");
					buf[1] = j;
					//System.out.println( "colonna: " + buf[1] + " \n");
					white.add(buf);							
				} else if (state.getPawn(i, j).equalsPawn("B")) {
					buf = new int[2];
					buf[0] = i;
					buf[1] = j;
					black.add(buf);
				}
			}
		}
		
		List<Action> actions = new ArrayList<Action>();
		Iterator<int[]> it=null;
		
		switch(state.getTurn()) {
			case WHITE:
				it = white.iterator();	//mi preparo per cercare tutte le mosse possibili per il bianco
				break;
			case BLACK:
				it = black.iterator();	//mi preparo per cercare tutte le mosse possibili per il nero
				break;
			default:		
				return actions;			//Nel caso in cui il turno sia BLACKWIN, WHITEWIN o DRAW restituisco la lista di azioni vuote (la partita non pu� proseguire dallo stato corrente)
		}

			
		//Arrivati qui � impossibile che l'Iterator it sia ancora null
		int colonna=0;
		int riga=0;

		Action action = null;
		try {
			action = new Action("z0", "z0", state.getTurn());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String fromString=null;
		String toString=null;
		int ctrl;

		while(it.hasNext()){
			buf=it.next();
			colonna=buf[1];  
			riga=buf[0];

			//System.out.println( "riga: " + buf[0] + " ");
			//System.out.println( "colonna: " + buf[1] + " \n");

			//tengo ferma la riga e muovo la colonna
			for (int j = 0; j < state.getBoard().length; j++){	    

				ctrl=0;
				ctrl=myCheckMove(colonna, j, riga, riga, ctrl, state);

				//se sono arrivato qui con ctrl=0 ho una mossa valida 
				if(ctrl==0){

					char colNew=(char)j;
					char colNewConverted=(char) Character.toLowerCase(colNew + 97);		

					char colOld=(char)colonna;
					char colonOldConverted=(char) Character.toLowerCase(colOld + 97);

					toString= new StringBuilder().append(colNewConverted).append(riga + 1).toString();	
					fromString= new StringBuilder().append(colonOldConverted).append(riga + 1).toString();

					//System.out.println("action da: " + fromString + " a " + toString + " \n");

					try {
						action = new Action(fromString, toString, state.getTurn());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					//System.out.println(action.toString() + "\n");
					actions.add(action);

				}
			}   
		    
		    //tengo ferma la colonna e muovo la riga

		    for (int i = 0; i < state.getBoard().length; i++){

		    	ctrl=0;

		    	ctrl= myCheckMove(colonna, colonna, riga, i, ctrl, state);

		    	//se sono arrivato qui con ctrl=0 ho una mossa valida 
		    	if(ctrl==0){

		    		char col=(char)colonna;
		    		char colConverted=(char) Character.toLowerCase(col + 97);

		    		toString= new StringBuilder().append(colConverted).append(i+1).toString();	
		    		fromString= new StringBuilder().append(colConverted).append(riga+1).toString();

		    		//System.out.println("action da: " + fromString + " a " + toString + " \n");

		    		try {
		    			action = new Action(fromString, toString, state.getTurn());
		    		} catch (IOException e) {
		    			// TODO Auto-generated catch block
		    			e.printStackTrace();
		    		}
		    		
		    		//System.out.println(action.toString() + "\n");
		    		actions.add(action);
		    	}
		    }
		}
		//System.out.println("tutte le possibili mosse: " + actions.toString());
		return actions;
	}

	@Override
	public State getInitialState() {
		return new StateTablut();
	}

	@Override
	public Turn getPlayer(State state) {
		return state.getTurn();
	}

	@Override
	public Turn[] getPlayers() {
		Turn[] players = new Turn[2];
		players[0]=Turn.BLACK;
		players[1]=Turn.WHITE;
		return players;
	}

	@Override
	public State getResult(State state, Action a) {
		return  myMovePawn(state.clone(), a);
			
			//return super.movePawn(state.clone(), a);
	}

	private State myMovePawn(State state, Action a) {
		//funzione di aggiornamento di uno stato data una azione
		State.Pawn[][] newBoard = state.getBoard();

		//metto nel nuovo tabellone la pedina mossa
		if (state.getTurn().equalsTurn("W")) {
			if(state.getPawn(a.getRowFrom(), a.getColumnFrom()).equalsPawn("K"))
				newBoard[a.getRowTo()][a.getColumnTo()]=State.Pawn.KING;
			else
				newBoard[a.getRowTo()][a.getColumnTo()]=State.Pawn.WHITE;
		} else /*if (state.getTurn().equalsTurn("B"))*/ {
			newBoard[a.getRowTo()][a.getColumnTo()]=State.Pawn.BLACK;
		}
				
		if(a.getColumnFrom()==4 && a.getRowFrom()==4)
			newBoard[a.getRowFrom()][a.getColumnFrom()]= State.Pawn.THRONE;
		else
			newBoard[a.getRowFrom()][a.getColumnFrom()]=State.Pawn.EMPTY;
		
		//aggiorno il tabellone
		 state.setBoard(newBoard);
		
		//effettuo eventuali catture 
		if (state.getTurn().equalsTurn("B")) {
			state = this.checkCaptureBlack(state, a);
		} else /*if (state.getTurn().equalsTurn("W"))*/ {
			state = this.checkCaptureWhite(state, a);
		}
		
		
		//cambio il turno
		if(state.getTurn().equalsTurn(State.Turn.WHITE.toString()))
			state.setTurn(State.Turn.BLACK);
		else if(state.getTurn().equalsTurn(State.Turn.BLACK.toString()))
			state.setTurn(State.Turn.WHITE);

		return state;		
	}
		
	//Codice by A.Fuschino
	@Override
	public boolean isTerminal(State state) {
		if(state.getTurn().equalsTurn("WW")|| state.getTurn().equalsTurn("BW") 
				|| state.getTurn().equalsTurn("D"))
			return true;
		return false;
	}

	// funzione euristica
	@Override
	public double getUtility(State state, Turn turn) {
		
		/*
		 * Primi controlli per tentare vincere/evitare di perdere:
		 * 1.Se sono il nero o sono il bianco e sto per vincere ritorno +infinito
		 * 2.Altrimenti se sono il bianco o sono il nero e sto per perdere ritorno -infinito
		 * Codice by L.Piazza
		 */
		if((turn.equalsTurn("B") && state.getTurn().equalsTurn("BW"))	|| (turn.equalsTurn("W") && state.getTurn().equalsTurn("WW")))
			return Double.POSITIVE_INFINITY;
		else if((turn.equalsTurn("B") && state.getTurn().equalsTurn("WW"))	|| (turn.equalsTurn("B") && state.getTurn().equalsTurn("BW")))
			return Double.NEGATIVE_INFINITY;	
		
		
		//conto le pedine
		List<int[]> white = new ArrayList<int[]>(); //tengo traccia della posizione nello stato dei bianchi
		int[] king =new int[2]; //tengo traccia della posizione del king
		List<int[]> black = new ArrayList<int[]>(); //uguale per i neri
		
		

		int[] buf; //mi indica la posizione ex."z6" 

		for (int i = 0; i < state.getBoard().length; i++) {
			for (int j = 0; j < state.getBoard().length; j++) {
				if (state.getPawn(i, j).equalsPawn("W") || state.getPawn(i, j).equalsPawn("K")) {
					if(state.getPawn(i, j).equalsPawn("K")){
						king[0]=i;
						king[1]=j;
					}
					buf = new int[2];
					buf[0] = i;
					//System.out.println( "riga: " + buf[0] + " ");
					buf[1] = j;
					//System.out.println( "colonna: " + buf[1] + " \n");
					white.add(buf);							
				} else if (state.getPawn(i, j).equalsPawn("B")) {
					buf = new int[2];
					buf[0] = i;
					buf[1] = j;
					black.add(buf);
				}
			}
		}
		
		double euristicaWhite= getHeuristicValueWhite(state,white, black, king);
		double euristicaBlack= getHeuristicValueBlack(state,white, black,king);
		if(turn.equalsTurn("W"))
			return euristicaWhite-euristicaBlack;
		else
			return euristicaBlack-euristicaWhite;
	}
	

	//euristica bianco
	private double getHeuristicValueWhite(State state,List<int[]> white, List<int[]> black, int[] king ) {
		int nWhite=white.size();
		double conteggioPedine=nWhite/9.0;
		double posKing=0;
		double pedineInAngolo=0;
		double scappaRe=0;
		int latiCopertiDelRe=0;
		int[] controlloPedine= {0,0};
		
		/*indice 0 riga, indice 1 colonna*/
		if((king[0]==2)&&( king[1]==2 ||king[1]==6))
			posKing=0.5;
		if((king[0]==6)&&( king[1]==2 ||king[1]==6))
			posKing=0.5;
		
		/*Pedine negli angoli*/
		for(int i=0;i<nWhite;i++){
			controlloPedine=white.get(i);
			if(controlloPedine[0]==0 && controlloPedine[1]==0)
				pedineInAngolo+=0.2;
			if(controlloPedine[0]==8 && controlloPedine[1]==8)
				pedineInAngolo+=0.2;
			if(controlloPedine[0]==8 && controlloPedine[1]==0 )
				pedineInAngolo+=0.2;
			if(controlloPedine[0]==0 && controlloPedine[1]==8)
				pedineInAngolo+=0.2;
		}
		
		/*Controllo che il re sia circondato da 3 pedine o da una casella "sponda" */
		for(int i=0;i<black.size();i++) {
			controlloPedine=black.get(i);
			//controllo a sinistra del re
			if(controlloPedine[0]==(king[0]-1) &&controlloPedine[1]==(king[1])) 
				latiCopertiDelRe++;
			//controllo a destra del re
			if(controlloPedine[0]==(1+king[0]) &&controlloPedine[1]==(king[1])) 
				latiCopertiDelRe++;
			//controllo a sopra del re
			if(controlloPedine[0]==(king[0]) &&controlloPedine[1]==(king[1]-1)) 
				latiCopertiDelRe++;
			//controllo a sotto del re
			if(controlloPedine[0]==(king[0]) &&controlloPedine[1]==(1+king[1])) 
				latiCopertiDelRe++;
		}
		/*controllo se il re � vicino al trono o a una casa del nero*/
		if((king[0]== 3 && king[1]==4)||(king[0]== 5 && king[1]==4)||(king[0]== 4 && king[1]==3)
				||(king[0]== 4 && king[1]==5)||(king[0]== 1 && king[1]==3)||(king[0]== 1 && king[1]==5)
				||(king[0]== 2 && king[1]==4)||(king[0]== 7 && king[1]==3)||(king[0]== 7 && king[1]==5)
				||(king[0]== 6 && king[1]==4)||(king[0]== 0 && king[1]==2)||(king[0]== 3 && king[1]==1)
				||(king[0]== 5 && king[1]==1)||(king[0]== 4 && king[1]==2)||(king[0]== 3 && king[1]==7)
				||(king[0]== 4 && king[1]==6)||(king[0]== 5 && king[1]==7))
			latiCopertiDelRe++;
		/*Se il re ha tre lati coperti penalizzo questa mossa (dovrebbe andare bene il valore negativo).*/
		if(latiCopertiDelRe==3)
			scappaRe=-1;
		
		double result= conteggioPedine+posKing+pedineInAngolo+scappaRe;
		return result;
	}
	
	//euristica nero
		private double getHeuristicValueBlack(State state,List<int[]> white, List<int[]> black, int[] king ) {
			int nBlack=black.size();
			double conteggioPedine=nBlack/16.0;
			
			double result=conteggioPedine;
			return result;
		}
	

		private State checkCaptureWhite(State state, Action a) {
			// controllo se mangio a destra
			if (a.getColumnTo() < state.getBoard().length - 2
					&& state.getPawn(a.getRowTo(), a.getColumnTo() + 1).equalsPawn("B")
					&& (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("W")
							|| state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("T")
							|| state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("K")
							|| (this.getCitadels().contains(state.getBox(a.getRowTo(), a.getColumnTo() + 2))
									&& !(a.getColumnTo() + 2 == 8 && a.getRowTo() == 4)
									&& !(a.getColumnTo() + 2 == 4 && a.getRowTo() == 0)
									&& !(a.getColumnTo() + 2 == 4 && a.getRowTo() == 8)
									&& !(a.getColumnTo() + 2 == 0 && a.getRowTo() == 4)))) {
				state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
			}
			// controllo se mangio a sinistra
			if (a.getColumnTo() > 1 && state.getPawn(a.getRowTo(), a.getColumnTo() - 1).equalsPawn("B")
					&& (state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("W")
							|| state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("T")
							|| state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("K")
							|| (this.getCitadels().contains(state.getBox(a.getRowTo(), a.getColumnTo() - 2))
									&& !(a.getColumnTo() - 2 == 8 && a.getRowTo() == 4)
									&& !(a.getColumnTo() - 2 == 4 && a.getRowTo() == 0)
									&& !(a.getColumnTo() - 2 == 4 && a.getRowTo() == 8)
									&& !(a.getColumnTo() - 2 == 0 && a.getRowTo() == 4)))) {
				state.removePawn(a.getRowTo(), a.getColumnTo() - 1);
			}
			// controllo se mangio sopra
			if (a.getRowTo() > 1 && state.getPawn(a.getRowTo() - 1, a.getColumnTo()).equalsPawn("B")
					&& (state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("W")
							|| state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("T")
							|| state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("K")
							|| (this.getCitadels().contains(state.getBox(a.getRowTo() - 2, a.getColumnTo()))
									&& !(a.getColumnTo() == 8 && a.getRowTo() - 2 == 4)
									&& !(a.getColumnTo() == 4 && a.getRowTo() - 2 == 0)
									&& !(a.getColumnTo() == 4 && a.getRowTo() - 2 == 8)
									&& !(a.getColumnTo() == 0 && a.getRowTo() - 2 == 4)))) {
				state.removePawn(a.getRowTo() - 1, a.getColumnTo());
			}
			// controllo se mangio sotto
			if (a.getRowTo() < state.getBoard().length - 2
					&& state.getPawn(a.getRowTo() + 1, a.getColumnTo()).equalsPawn("B")
					&& (state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("W")
							|| state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("T")
							|| state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("K")
							|| (this.getCitadels().contains(state.getBox(a.getRowTo() + 2, a.getColumnTo()))
									&& !(a.getColumnTo() == 8 && a.getRowTo() + 2 == 4)
									&& !(a.getColumnTo() == 4 && a.getRowTo() + 2 == 0)
									&& !(a.getColumnTo() == 4 && a.getRowTo() + 2 == 8)
									&& !(a.getColumnTo() == 0 && a.getRowTo() + 2 == 4)))) {
				state.removePawn(a.getRowTo() + 1, a.getColumnTo());
			}
			// controllo se ho vinto
			if (a.getRowTo() == 0 || a.getRowTo() == state.getBoard().length - 1 || a.getColumnTo() == 0
					|| a.getColumnTo() == state.getBoard().length - 1) {
				if (state.getPawn(a.getRowTo(), a.getColumnTo()).equalsPawn("K")) {
					state.setTurn(State.Turn.WHITEWIN);
				}
			}
			// TODO: implement the winning condition of the capture of the last
			// black checker

			return state;
		}

		private State checkCaptureBlackKingLeft(State state, Action a) {
			// ho il re sulla sinistra
			if (a.getColumnTo() > 1 && state.getPawn(a.getRowTo(), a.getColumnTo() - 1).equalsPawn("K")) {
				// re sul trono
				if (state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e5")) {
					if (state.getPawn(3, 4).equalsPawn("B") && state.getPawn(4, 3).equalsPawn("B")
							&& state.getPawn(5, 4).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				// re adiacente al trono
				if (state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e4")) {
					if (state.getPawn(2, 4).equalsPawn("B") && state.getPawn(3, 3).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);					
					}
				}
				if (state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("f5")) {
					if (state.getPawn(5, 5).equalsPawn("B") && state.getPawn(3, 5).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				if (state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e6")) {
					if (state.getPawn(6, 4).equalsPawn("B") && state.getPawn(5, 3).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				// sono fuori dalle zone del trono
				if (!state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e5")
						&& !state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e6")
						&& !state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e4")
						&& !state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("f5")) {
					if (state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("B")
							|| this.getCitadels().contains(state.getBox(a.getRowTo(), a.getColumnTo() - 2))) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
			}
			return state;
		}

		private State checkCaptureBlackKingRight(State state, Action a) {
			// ho il re sulla destra
			if (a.getColumnTo() < state.getBoard().length - 2
					&& (state.getPawn(a.getRowTo(), a.getColumnTo() + 1).equalsPawn("K"))) {
				// re sul trono
				if (state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e5")) {
					if (state.getPawn(3, 4).equalsPawn("B") && state.getPawn(4, 5).equalsPawn("B")
							&& state.getPawn(5, 4).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				// re adiacente al trono
				if (state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e4")) {
					if (state.getPawn(2, 4).equalsPawn("B") && state.getPawn(3, 5).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				if (state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e6")) {
					if (state.getPawn(5, 5).equalsPawn("B") && state.getPawn(6, 4).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				if (state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("d5")) {
					if (state.getPawn(3, 3).equalsPawn("B") && state.getPawn(5, 3).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				// sono fuori dalle zone del trono
				if (!state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("d5")
						&& !state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e6")
						&& !state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e4")
						&& !state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e5")) {
					if (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("B")
							|| this.getCitadels().contains(state.getBox(a.getRowTo(), a.getColumnTo() + 2))) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
			}
			return state;
		}

		private State checkCaptureBlackKingDown(State state, Action a) {
			// ho il re sotto
			if (a.getRowTo() < state.getBoard().length - 2
					&& state.getPawn(a.getRowTo() + 1, a.getColumnTo()).equalsPawn("K")) {
				// re sul trono
				if (state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("e5")) {
					if (state.getPawn(5, 4).equalsPawn("B") && state.getPawn(4, 5).equalsPawn("B")
							&& state.getPawn(4, 3).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				// re adiacente al trono
				if (state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("e4")) {
					if (state.getPawn(3, 3).equalsPawn("B") && state.getPawn(3, 5).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				if (state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("d5")) {
					if (state.getPawn(4, 2).equalsPawn("B") && state.getPawn(5, 3).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				if (state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("f5")) {
					if (state.getPawn(4, 6).equalsPawn("B") && state.getPawn(5, 5).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				// sono fuori dalle zone del trono
				if (!state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("d5")
						&& !state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("e4")
						&& !state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("f5")
						&& !state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("e5")) {
					if (state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("B")
							|| this.getCitadels().contains(state.getBox(a.getRowTo() + 2, a.getColumnTo()))) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
			}
			return state;
		}

		private State checkCaptureBlackKingUp(State state, Action a) {
			// ho il re sopra
			if (a.getRowTo() > 1 && state.getPawn(a.getRowTo() - 1, a.getColumnTo()).equalsPawn("K")) {
				// re sul trono
				if (state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("e5")) {
					if (state.getPawn(3, 4).equalsPawn("B") && state.getPawn(4, 5).equalsPawn("B")
							&& state.getPawn(4, 3).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				// re adiacente al trono
				if (state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("e6")) {
					if (state.getPawn(5, 3).equalsPawn("B") && state.getPawn(5, 5).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
						
					}
				}
				if (state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("d5")) {
					if (state.getPawn(4, 2).equalsPawn("B") && state.getPawn(3, 3).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				if (state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("f5")) {
					if (state.getPawn(4, 6).equalsPawn("B") && state.getPawn(3, 5).equalsPawn("B")) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
				// sono fuori dalle zone del trono
				if (!state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("d5")
						&& !state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("e4")
						&& !state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("f5")
						&& !state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("e5")) {
					if (state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("B")
							|| this.getCitadels().contains(state.getBox(a.getRowTo() - 2, a.getColumnTo()))) {
						state.setTurn(State.Turn.BLACKWIN);
					}
				}
			}
			return state;
		}

		private State checkCaptureBlackPawnRight(State state, Action a) {
			// mangio a destra
			if (a.getColumnTo() < state.getBoard().length - 2
					&& state.getPawn(a.getRowTo(), a.getColumnTo() + 1).equalsPawn("W")) {
				if (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("B")) {
					state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
				}
				if (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("T")) {
					state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
				}
				if (this.getCitadels().contains(state.getBox(a.getRowTo(), a.getColumnTo() + 2))) {
					state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
				}
				if (state.getBox(a.getRowTo(), a.getColumnTo() + 2).equals("e5")) {
					state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
				}

			}

			return state;
		}

		private State checkCaptureBlackPawnLeft(State state, Action a) {
			// mangio a sinistra
			if (a.getColumnTo() > 1 && state.getPawn(a.getRowTo(), a.getColumnTo() - 1).equalsPawn("W")
					&& (state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("B")
							|| state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("T")
							|| this.getCitadels().contains(state.getBox(a.getRowTo(), a.getColumnTo() - 2))
							|| (state.getBox(a.getRowTo(), a.getColumnTo() - 2).equals("e5")))) {
				state.removePawn(a.getRowTo(), a.getColumnTo() - 1);
			}
			return state;
		}

		private State checkCaptureBlackPawnUp(State state, Action a) {
			// controllo se mangio sopra
			if (a.getRowTo() > 1 && state.getPawn(a.getRowTo() - 1, a.getColumnTo()).equalsPawn("W")
					&& (state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("B")
							|| state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("T")
							|| this.getCitadels().contains(state.getBox(a.getRowTo() - 2, a.getColumnTo()))
							|| (state.getBox(a.getRowTo() - 2, a.getColumnTo()).equals("e5")))) {
				state.removePawn(a.getRowTo() - 1, a.getColumnTo());
			}
			return state;
		}

		private State checkCaptureBlackPawnDown(State state, Action a) {
			// controllo se mangio sotto
			if (a.getRowTo() < state.getBoard().length - 2
					&& state.getPawn(a.getRowTo() + 1, a.getColumnTo()).equalsPawn("W")
					&& (state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("B")
							|| state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("T")
							|| this.getCitadels().contains(state.getBox(a.getRowTo() + 2, a.getColumnTo()))
							|| (state.getBox(a.getRowTo() + 2, a.getColumnTo()).equals("e5")))) {
				state.removePawn(a.getRowTo() + 1, a.getColumnTo());
			}
			return state;
		}

		private State checkCaptureBlack(State state, Action a) {

			this.checkCaptureBlackPawnRight(state, a);
			this.checkCaptureBlackPawnLeft(state, a);
			this.checkCaptureBlackPawnUp(state, a);
			this.checkCaptureBlackPawnDown(state, a);
			this.checkCaptureBlackKingRight(state, a);
			this.checkCaptureBlackKingLeft(state, a);
			this.checkCaptureBlackKingDown(state, a);
			this.checkCaptureBlackKingUp(state, a);

			
			return state;
		}


}
