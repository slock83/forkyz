
package app.crossword.yourealwaysbe.util;

import java.util.ArrayList;
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
        // list of starter keywords
        private List<String> commands = new ArrayList<>();
        private Consumer<String> execute;

        /**
         * Construct a VoiceCommand
         *
         * @param commandId the R.string id for the word identifying
         * this command
         * @param execute a function that consumes the string after the
         * command word
         */
        public VoiceCommand(String command, @NonNull Consumer<String> execute
        ) {
            this.commands.add(command);
            this.execute = execute;
        }

        public VoiceCommand(
            String command, String commandAlt, @NonNull Consumer<String> execute
        ) {
            this.commands.add(command);
            this.commands.add(commandAlt);
            this.execute = execute;
        }

        /**
         * If s matches the command trigger word
         *
         * Ignores case
         */
        public boolean matches(String s) {
            for (String command : commands) {
                if (command.equalsIgnoreCase(s))
                    return true;
            }
            return false;
        }

        /**
         * Execute the command on board with additional input args
         */
        public void execute(String args) {
            execute.accept(args);
        }
    }
}
