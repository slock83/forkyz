
package app.crossword.yourealwaysbe.util;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import androidx.annotation.NonNull;

/**
 * Interpreting and dispatching voice commands
 */
public class VoiceCommands {

    private List<VoiceCommand> commands = new LinkedList<>();

    public void registerVoiceCommand(@NonNull VoiceCommand command) {
        commands.add(command);
    }

    /**
     * Execute the result from speech service
     *
     * @param commandAlternatives list of speech result strings, each
     * considered to be possible alternatives, ordered by likelihood.
     * Dispatch searches through them in order until it finds a match.
     */
    public void dispatch(List<String> commandAlternatives) {
        if (commandAlternatives == null)
            return;

        for (String command : commandAlternatives) {
            if (command == null)
                continue;

            String[] split = command.split("\\W+", 2);
            if (split.length < 1)
                return;

            String key = split[0];
            String args = (split.length < 2) ? "" : split[1];

            for (VoiceCommand vc : commands) {
                if (vc.matches(key)) {
                    vc.execute(args);
                    return;
                }
            }
        }
    }

    public static class VoiceCommand {
        private String command;
        private Consumer<String> execute;

        /**
         * Construct a VoiceCommand
         *
         * @param commandId the R.string id for the word identifying
         * this command
         * @param execute a function that consumes the string after the
         * command word
         */
        public VoiceCommand(String command, @NonNull Consumer<String> execute) {
            this.command = command;
            this.execute = execute;
        }

        /**
         * If s matches the command trigger word
         *
         * Ignores case
         */
        public boolean matches(String s) {
            return command.equalsIgnoreCase(s);
        }

        /**
         * Execute the command on board with additional input args
         */
        public void execute(String args) {
            execute.accept(args);
        }
    }
}
