package com.harsh.shah.threads.clone.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class PollOptions implements Parcelable {
    public static final Creator<PollOptions> CREATOR = new Creator<PollOptions>() {
        @Override
        public PollOptions createFromParcel(Parcel in) {
            return new PollOptions(in);
        }

        @Override
        public PollOptions[] newArray(int size) {
            return new PollOptions[size];
        }
    };
    private PollOptionsItem option3;
    private PollOptionsItem option4;
    private PollOptionsItem option1;
    private PollOptionsItem option2;

    public PollOptions() {
    }

    protected PollOptions(Parcel in) {
        option3 = in.readParcelable(PollOptionsItem.class.getClassLoader());
        option4 = in.readParcelable(PollOptionsItem.class.getClassLoader());
        option1 = in.readParcelable(PollOptionsItem.class.getClassLoader());
        option2 = in.readParcelable(PollOptionsItem.class.getClassLoader());
    }

    public PollOptions(PollOptionsItem option1, PollOptionsItem option2, PollOptionsItem option3, PollOptionsItem option4) {
        this.option3 = option3;
        this.option4 = option4;
        this.option1 = option1;
        this.option2 = option2;
    }

    public PollOptionsItem getOption3() {
        return option3;
    }

    public void setOption3(PollOptionsItem option3) {
        this.option3 = option3;
    }

    public PollOptionsItem getOption4() {
        return option4;
    }

    public void setOption4(PollOptionsItem option4) {
        this.option4 = option4;
    }

    public PollOptionsItem getOption1() {
        return option1;
    }

    public void setOption1(PollOptionsItem option1) {
        this.option1 = option1;
    }

    public PollOptionsItem getOption2() {
        return option2;
    }

    public void setOption2(PollOptionsItem option2) {
        this.option2 = option2;
    }

    @NonNull
    @Override
    public String toString() {
        return
                "PollOptions{" +
                        "option3 = '" + option3 + '\'' +
                        ",option4 = '" + option4 + '\'' +
                        ",option1 = '" + option1 + '\'' +
                        ",option2 = '" + option2 + '\'' +
                        "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeParcelable(option3, i);
        parcel.writeParcelable(option4, i);
        parcel.writeParcelable(option1, i);
        parcel.writeParcelable(option2, i);
    }


    public static class PollOptionsItem implements Parcelable{
        private String text = "";
        private boolean visibility = false;
        private ArrayList<String> votes = new ArrayList<>();

        public PollOptionsItem() {
        }

        public PollOptionsItem(ArrayList<String> votes, String text, boolean visibility) {
            this.votes = votes;
            this.text = text;
            this.visibility = visibility;
        }

        protected PollOptionsItem(Parcel in) {
            votes = in.createStringArrayList();
            text = in.readString();
            visibility = in.readByte() != 0;
        }

        public static final Creator<PollOptionsItem> CREATOR = new Creator<PollOptionsItem>() {
            @Override
            public PollOptionsItem createFromParcel(Parcel in) {
                return new PollOptionsItem(in);
            }

            @Override
            public PollOptionsItem[] newArray(int size) {
                return new PollOptionsItem[size];
            }
        };

        public ArrayList<String> getVotes() {
            return votes;
        }

        public void setVotes(ArrayList<String> votes) {
            this.votes = votes;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean getVisibility() {
            return visibility;
        }

        public void setVisibility(boolean visibility) {
            this.visibility = visibility;
        }

        @NonNull
        @Override
        public String toString() {
            return
                    "Option{" +
                            "votes = '" + votes + '\'' +
                            ",text = '" + text + '\'' +
                            ",visibility = '" + visibility + '\'' +
                            "}";
        }

        @Override
        public int describeContents() {
                return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel parcel, int i) {
            parcel.writeStringList(votes);
            parcel.writeString(text);
            parcel.writeByte((byte) (visibility ? 1 : 0));
        }
    }

}
