package github.kakarot.Raids.Game;

public enum GameState {
    LOBBY, //Players are waiting for the first wave to spawn
    WAVE_IN_PROGRESS, //There's a wave in progress and players are fighting it
    WAVE_COOLDOWN, //A wave was just finished and players are waiting for next wave
    ENDING; //The game is ending
}
