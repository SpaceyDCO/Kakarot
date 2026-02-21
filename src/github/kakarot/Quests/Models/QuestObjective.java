package github.kakarot.Quests.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
public class QuestObjective {
    private final ObjectiveType type;
    private ObjectiveInfo objectiveInfo;
    private TrackingInfo trackingInfo;
    private final int required;
    private final boolean shareable;
    public QuestObjective(ObjectiveType type, ObjectiveInfo objectiveInfo, int required, boolean shareable) {
        this.type = type;
        this.objectiveInfo = objectiveInfo;
        this.trackingInfo = null;
        this.required = required;
        this.shareable = shareable;
    }
    public QuestObjective(ObjectiveType type, ObjectiveInfo objectiveInfo, TrackingInfo trackingInfo, int required, boolean shareable) {
        this.type = type;
        this.objectiveInfo = objectiveInfo;
        TrackingInfo trackInfo = new TrackingInfo();
        trackInfo.setX(trackingInfo.getX());
        trackInfo.setY(trackingInfo.getY());
        trackInfo.setZ(trackingInfo.getZ());
        if(trackingInfo.getLabel() == null || trackingInfo.getLabel().isEmpty()) trackInfo.setLabel("");
        else trackInfo.setLabel(trackingInfo.getLabel());
        if(trackingInfo.getArrowColor() == null || trackingInfo.getArrowColor().isEmpty()) trackInfo.setArrowColor("white");
        else trackInfo.setArrowColor(trackingInfo.getArrowColor());
        if(trackingInfo.getLabelColor() == null || trackingInfo.getLabelColor().isEmpty()) trackInfo.setLabelColor("white");
        else trackInfo.setLabelColor(trackingInfo.getLabelColor());
        this.trackingInfo = trackInfo;
        this.required = required;
        this.shareable = shareable;
    }
    public boolean isComplete(int currentProgress) {
        return currentProgress >= this.required;
    }
    public int getProgressCompletedAsPercentage(int currentProgress) {
        return Math.min(100, (currentProgress * 100) / this.required);
    }
}
