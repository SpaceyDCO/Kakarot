package github.kakarot.Tools.Commands;

import github.kakarot.Main;

import java.io.IOException;

public abstract class BaseCommand {

    public Main main = Main.instance;

    public BaseCommand() {
        main.getCommandFramework().registerCommands(this);
    }

    public abstract void onCommand(CommandArgs command) throws IOException;

}
