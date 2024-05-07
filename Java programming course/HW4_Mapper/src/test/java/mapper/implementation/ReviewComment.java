package mapper.implementation;

import ru.hse.homework4.Exported;
import ru.hse.homework4.PropertyName;

@Exported
public class ReviewComment {
    @PropertyName("opinion")
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "ReviewComment{" +
                "comment='" + comment + '\'' +
                '}';
    }
}