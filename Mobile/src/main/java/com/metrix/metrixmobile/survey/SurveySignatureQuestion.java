package com.metrix.metrixmobile.survey;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.signature.SignatureField;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

import java.util.HashMap;

public class SurveySignatureQuestion extends SurveyQuestion{
    private TextView mQuestion;
    public SignatureField mSignatureField;
    private Button mNext;

    public SurveySignatureQuestion(Context context) {
        super(context);
    }

    public SurveySignatureQuestion(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        setContentView(R.layout.survey_signature_question);

        mQuestion = (TextView)this.findViewById(R.id.survey_sigq_question);

        mSignatureField = (SignatureField) this.findViewById(R.id.survey_sigq_field);

        MetrixColumnDef staticSurveyColumnDef = new MetrixColumnDef();
        staticSurveyColumnDef.readOnlyInMetadata = false;
        staticSurveyColumnDef.allowClear = true;
        staticSurveyColumnDef.readOnly = false;
        staticSurveyColumnDef.messageId = "SurveySignature";
        staticSurveyColumnDef.transactionIdTableName = "task";
        staticSurveyColumnDef.transactionIdColumnName = "task_id";
        mSignatureField.setupFromConfiguration(mActivityInfo, staticSurveyColumnDef, "survey_result");

        mSignatureField.setIsSurvey();

        mSignatureField.mHiddenAttachmentIdTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateNextEnabled();
            }
        });

        mNext = (Button)this.findViewById(R.id.survey_sigq_continue);
        mNext.setText(AndroidResourceHelper.getMessage("Next"));
        mNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                next();
            }
        });
    }

    @Override
    protected void onQuestionChanged(SurveyQuestionData newQuestion) {
        super.onQuestionChanged(newQuestion);

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
        
        String questionId = question.getQuestionId();
        mSignatureField.setSurveyQuestionId(questionId);

        boolean enabled = false;

        if (question.isOptional())
            enabled = true;

        String answer = mSignatureField.mHiddenAttachmentIdTextView.getText().toString();
        if (!MetrixStringHelper.isNullOrEmpty(answer) && !MetrixStringHelper.valueIsEqual(answer, "0"))
            enabled = true;
        mNext.setEnabled(enabled);
    }

    private void next() {
        String answer = mSignatureField.mHiddenAttachmentIdTextView.getText().toString();

        getAnswer().setAttachmentId(MetrixStringHelper.isNullOrEmpty(answer) ? 0 : Integer.parseInt(answer));

        if(MetrixPublicCache.instance.containsKey("LaunchingSignatureFieldValue"))
            MetrixPublicCache.instance.removeItem("LaunchingSignatureFieldValue");

        setIsCompleted(true);
    }

    @Override
    protected void onAnswerChanged(SurveyAnswerData answer) {
        super.onAnswerChanged(answer);
        mSignatureField.mHiddenAttachmentIdTextView.setText(String.valueOf(answer.getAttachmentId()));
        mSignatureField.updateFieldUI();
    }

    @Override
    protected void onIsCompletedChanged(boolean isComplete) {
        super.onIsCompletedChanged(isComplete);
        mNext.setVisibility(isComplete ? GONE : VISIBLE);
        mSignatureField.setEnabled(!isComplete);
    }
}
