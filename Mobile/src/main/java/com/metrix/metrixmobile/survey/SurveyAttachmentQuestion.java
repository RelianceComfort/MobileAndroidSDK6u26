package com.metrix.metrixmobile.survey;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.metrix.architecture.attachment.AttachmentField;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

public class SurveyAttachmentQuestion extends SurveyQuestion {
    private TextView mQuestion;
    private AttachmentField mAttachmentField;
    private Button mNext;

    public SurveyAttachmentQuestion(Context context) { super(context); }
    public SurveyAttachmentQuestion(Context context, AttributeSet attrs) { super(context, attrs); }

    public void onCreate() {
        super.onCreate();

        setContentView(R.layout.survey_attachment_question);

        mQuestion = (TextView)this.findViewById(R.id.survey_attachq_question);

        mAttachmentField = (AttachmentField) this.findViewById(R.id.survey_attachq_field);

        MetrixColumnDef staticSurveyColumnDef = new MetrixColumnDef();
        staticSurveyColumnDef.readOnlyInMetadata = false;
        staticSurveyColumnDef.allowPhoto = true;
        staticSurveyColumnDef.allowVideo = true;
        staticSurveyColumnDef.allowFile = true;
        staticSurveyColumnDef.transactionIdTableName = "task";
        staticSurveyColumnDef.transactionIdColumnName = "task_id";
        mAttachmentField.setupFromConfiguration(mActivityInfo, staticSurveyColumnDef, "survey_result");

        updateAttachmentFieldBackground();

        mAttachmentField.mHiddenAttachmentIdTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                String answer = mAttachmentField.mHiddenAttachmentIdTextView.getText().toString();
                getAnswer().setAttachmentId(MetrixStringHelper.isNullOrEmpty(answer) ? 0 : Integer.parseInt(answer));
                updateNextEnabled();
            }
        });

        mNext = (Button)this.findViewById(R.id.survey_attachq_continue);
        mNext.setText(AndroidResourceHelper.getMessage("Next"));
        mNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { next(); }
        });
    }

    @Override
    protected void onQuestionChanged(SurveyQuestionData newQuestion) {
        super.onQuestionChanged(newQuestion);

        mAttachmentField.setSurveyQuestionId(newQuestion.getQuestionId());

        String continueText = newQuestion.getContinueText();
        if (continueText == null) {
            continueText = AndroidResourceHelper.getMessage("Next");
        }

        mNext.setText(continueText);

        mQuestion.setText(newQuestion.getQuestion());
        updateNextEnabled();
    }

    private void updateNextEnabled() {
        SurveyQuestionData question = getQuestion();

        if (question == null)
            return;

        boolean enabled = false;

        if (question.isOptional())
            enabled = true;

        String answer = mAttachmentField.mHiddenAttachmentIdTextView.getText().toString();
        if (!MetrixStringHelper.isNullOrEmpty(answer) && !MetrixStringHelper.valueIsEqual(answer, "0"))
            enabled = true;

        mNext.setEnabled(enabled);
    }

    private void next() {
        String answer = mAttachmentField.mHiddenAttachmentIdTextView.getText().toString();

        getAnswer().setAttachmentId(MetrixStringHelper.isNullOrEmpty(answer) ? 0 : Integer.parseInt(answer));

        setIsCompleted(true);
    }

    @Override
    protected void onAnswerChanged(SurveyAnswerData answer) {
        super.onAnswerChanged(answer);

        mAttachmentField.mHiddenAttachmentIdTextView.setText(String.valueOf(answer.getAttachmentId()));
        mAttachmentField.updateFieldUI();
    }

    @Override
    protected void onIsCompletedChanged(boolean isComplete) {
        super.onIsCompletedChanged(isComplete);
        mNext.setVisibility(isComplete ? GONE : VISIBLE);
        mAttachmentField.setEnabled(!isComplete);

        updateAttachmentFieldBackground();
    }

    private void updateAttachmentFieldBackground() {
        if (!mIsCompleted) {
            if (!MetrixStringHelper.isNullOrEmpty(mLighterColorString)) {
                mAttachmentField.setBackgroundForSurvey(mLighterColorString);
            } else {
                mAttachmentField.setBackgroundForSurvey("F3EEF3");
            }
        } else {
            mAttachmentField.setBackgroundForSurvey("EEEEEE");
        }
    }
}
