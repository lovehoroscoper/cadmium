package com.meltmedia.cadmium.cli;

import com.beust.jcommander.JCommander;

public class CadmiumCli {
	
	public static JCommander jCommander = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
		  MainCommands mainCommands = new MainCommands();
		  //jCommander = new JCommander(mainCommands);
		  jCommander = new JCommander();
		  
		  jCommander.setProgramName("cadmium");
		  
		  UpdateCommand updateCommand = new UpdateCommand();
		  jCommander.addCommand("update", updateCommand);
		  
		  HelpCommand helpCommand = new HelpCommand();
		  jCommander.addCommand("help", helpCommand);
		  
		  InitializeCommand initCommand = new InitializeCommand();
		  jCommander.addCommand("init", initCommand);
		  
		  jCommander.parse(args);
		  
		  String commandName = jCommander.getParsedCommand();
		  if( commandName == null ) {
			  System.out.println("Use cadmium help for usage information.");
			  return;
		  }
		  
		  if( commandName.equals("help") ) {
			  if( helpCommand.subCommand.size()==0 ) {
				  jCommander.usage();
				  return;
			  }
			  else {
				 JCommander subCommander = jCommander.getCommands().get(helpCommand.subCommand.get(0));
				 if( subCommander == null ) {
					 System.out.println("Unknown sub command "+helpCommand.subCommand);
					 return;
				 }
				 subCommander.usage();
				 return;
			  }
		  }
		  else if( commandName.equals("update") ) {
		     throw new UnsupportedOperationException("update not yet supported.");
		  }
		  else if( commandName.equals("init") ) {
		    initCommand.execute();
		  }
		
		}
		catch( Exception e ) {
			jCommander.usage();
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

	}

}