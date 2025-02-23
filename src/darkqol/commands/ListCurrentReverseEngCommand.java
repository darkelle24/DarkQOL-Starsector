package darkqol.commands;

import java.util.HashMap;
import java.util.Map;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import darkqol.ids.Ids;
import darkqol.utils.SaveOneData;

public class ListCurrentReverseEngCommand implements BaseCommand {
    private SaveOneData<Map<String, Float>> reverseEngProgressList;

    public ListCurrentReverseEngCommand() {
        super();
        this.reverseEngProgressList = new SaveOneData<Map<String, Float>>(Ids.REVERSE_ENG_MEMORY,
                new HashMap<String, Float>());
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        String researchList = getResearchList();
        Console.showMessage("List of Reverse Engineering :\n" + researchList);
        return CommandResult.SUCCESS;
    }

    private String getResearchList() {
        Map<String, Float> researchData = reverseEngProgressList.getData();
        if (researchData.isEmpty()) {
            return "No reverse engineering in progress.";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Float> entry : researchData.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue() * 100).append("%\n");
        }

        return sb.toString();
    }
}
