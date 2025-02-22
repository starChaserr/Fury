package app.personal.fury.UI.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import app.personal.MVVM.Entity.LaunchChecker;
import app.personal.MVVM.Entity.balanceEntity;
import app.personal.MVVM.Entity.inHandBalEntity;
import app.personal.MVVM.Entity.salaryEntity;
import app.personal.MVVM.Viewmodel.AppUtilViewModel;
import app.personal.MVVM.Viewmodel.mainViewModel;
import app.personal.Utls.Commons;
import app.personal.Utls.Constants;
import app.personal.Utls.TutorialUtil;
import app.personal.fury.R;
import app.personal.fury.UI.Adapters.salaryList.salaryAdapter;
import app.personal.fury.UI.MainActivity;
import app.personal.fury.UI.Adapters.infoGraphicAdapter.infoGraphicsAdapter;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class Ear_Tracker extends Fragment {
    //Daily = 1, Monthly = 0, Hourly = -1, oneTime = ?(To be implemented in a future update).
    private RecyclerView salSplitList;
    private FloatingActionButton addSal;
    private mainViewModel vm;
    private salaryAdapter adapter;
    private Button yes;
    private final ArrayList<View> Targets = new ArrayList<>();
    private final ArrayList<String> PrimaryTexts = new ArrayList<>();
    private final ArrayList<String> SecondaryTexts = new ArrayList<>();
    private TextView SalAmt, inHandAmt, accountAmt, inHandCount, accountCount;
    private int budType;
    private inHandBalEntity inHandBal = new inHandBalEntity();
    private balanceEntity balanceEntity = new balanceEntity();
    private AdView ad;
    private LinearLayout adLayout;
    private AdRequest adRequest;
    private String budDate, Currency = "";
    private final int[] FragmentList =
            new int[]{R.drawable.info_h1, R.drawable.info_h2,
                    R.drawable.info_h3, R.drawable.info_h4,
                    R.drawable.info_h5, R.drawable.info_h6};
    private int cashAmt, cashCount, accAmt, accCount, totalExp, totalSalary;
    private TutorialUtil util;
    private AppUtilViewModel appVm;
    private boolean loaded = false;
    private final MutableLiveData<Boolean> isVisible = new MutableLiveData<>();

    public Ear_Tracker() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(mainViewModel.class);
        vm.getRupee().observe(requireActivity(), String -> {
            if (String != null || !String.getCurrency().equals("")) {
                Currency = String.getCurrency();
            } else {
                vm.initCurrency();
            }
        });
        if (savedInstanceState == null) {
            MobileAds.initialize(requireContext());
            adRequest = new AdRequest.Builder().build();
        }
        appVm = new ViewModelProvider(requireActivity()).get(AppUtilViewModel.class);
        adapter = new salaryAdapter();
        inHandBal = getInHandBal();
        balanceEntity = getBal();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.main_fragment_earningstracker, container, false);
        findView(v);
        return v;
    }

    private void requestAd() {
        try {
            ad.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    adLayout.setVisibility(View.GONE);
                    new CountDownTimer(5000, 1000) {

                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            isVisible.observe(requireActivity(), Boolean -> {
                                if (Boolean) {
                                    loadAd();
                                }
                            });
                        }
                    };
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    adLayout.setVisibility(View.VISIBLE);
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void loadAd() {
        if (!loaded) {
            ad.loadAd(adRequest);
            adLayout.setVisibility(View.GONE);
            loaded = true;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String s = Currency + getSalary();
        SalAmt.setText(s);
        getExp();
        isVisible.observe(requireActivity(), Boolean -> {
            if (Boolean) {
                if (savedInstanceState == null) {
                    loadAd();
                    requestAd();
                }
            }
        });
        util = new TutorialUtil(requireActivity(), requireContext(), requireActivity(), requireActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
        loaded = false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        isVisible.postValue(isVisibleToUser);
        if (isVisibleToUser) {
            if (getUserVisibleHint()) {
                try {
                    TutorialPhase2();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void findView(View v) {
        SalAmt = v.findViewById(R.id.salAmt);
        salSplitList = v.findViewById(R.id.salList);
        addSal = v.findViewById(R.id.addSal);
        addSal.setOnClickListener(v1 -> {
            inHandBal = getInHandBal();
            balanceEntity = getBal();
            callPopUpWindow(false, null);
        });
        salSplitList.setLayoutManager(new LinearLayoutManager(requireContext()));
        salSplitList.setHasFixedSize(true);
        ad = v.findViewById(R.id.adView);
        adLayout = v.findViewById(R.id.adLayout);
        salSplitList.setAdapter(adapter);
        inHandAmt = v.findViewById(R.id.inhand_Amt);
        inHandCount = v.findViewById(R.id.inhand_count);
        accountAmt = v.findViewById(R.id.account_amt);
        accountCount = v.findViewById(R.id.account_count);

        ViewPager ig_vp = v.findViewById(R.id.infoGraphics_earvp);
        TabLayout ig_tl = v.findViewById(R.id.infoGraphics_ear);
        infoGraphicsAdapter igAdapter = new infoGraphicsAdapter(requireContext(), FragmentList);
        ig_vp.setAdapter(igAdapter);
        ig_tl.setupWithViewPager(ig_vp, true);
        Commons.timedSliderInit(ig_vp, FragmentList, 5);
        touchHelper();
    }

    @SuppressLint("SetTextI18n")
    private void callPopUpWindow(boolean isEdit, @Nullable salaryEntity salary) {
        PopupWindow popupWindow = new PopupWindow(getContext());
        LayoutInflater inflater = (LayoutInflater) requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        View v = inflater.inflate(R.layout.add_item_exp, null);
        popupWindow.setContentView(v);

//        Total elements--------------------------------------------------
        TextView popupTitle, salModeTitle;
        FrameLayout expName;
        EditText salSource, salAmt, salDate;
        RadioGroup rdGrp1, rdGrp2;
        Button no;
        String date;
//        ----------------------------------------------------------------
//        Init Views------------------------------------------------------
        popupTitle = v.findViewById(R.id.title);
        popupTitle.setText("Add Earnings");
        LinearLayout bal = v.findViewById(R.id.cashDetails);
        bal.setVisibility(View.GONE);
        expName = v.findViewById(R.id.expNameTitle);
        expName.setVisibility(View.GONE);
        salDate = v.findViewById(R.id.salDate);
        salSource = v.findViewById(R.id.salSrc);
        salAmt = v.findViewById(R.id.expAmt);
        rdGrp2 = v.findViewById(R.id.RadioGroup2);
        rdGrp1 = v.findViewById(R.id.RadioGroup);
        yes = v.findViewById(R.id.add_yes);
        no = v.findViewById(R.id.add_no);
        no.setOnClickListener(v1 -> popupWindow.dismiss());
        salModeTitle = v.findViewById(R.id.radioTitle2);
        TextView unwanted = v.findViewById(R.id.expAmtDisp);

//        ----------------------------------------------------------------
        if (!isEdit) {
            salDate.setVisibility(View.GONE);
            unwanted.setVisibility(View.GONE);
            date = Commons.getDate();
        } else {
            assert salary != null;
            unwanted.setVisibility(View.VISIBLE);
            String s = Currency + salary.getSalary();
            unwanted.setText(s);

            rdGrp2.setVisibility(View.GONE);
            salModeTitle.setVisibility(View.GONE);
            salSource.setText(salary.getIncName());
            salAmt.setVisibility(View.GONE);
            date = salary.getCreationDate();
            salDate.setText(date);
            switch (salary.getSalMode()) {
                case Constants.SAL_MODE_ACC:
                    rdGrp2.check(R.id.account);
                    break;
                case Constants.SAL_MODE_CASH:
                    rdGrp2.check(R.id.inHand);
                    break;
                default:
                    break;
            }
            switch (salary.getIncType()) {
                case Constants.daily:
                    rdGrp1.check(R.id.daily);
                    break;
                case Constants.monthly:
                    rdGrp1.check(R.id.monthly);
                    break;
                case Constants.oneTime:
                    rdGrp1.check(R.id.oneTime);
                    break;
                default:
                    break;
            }
        }

        yes.setOnClickListener(v1 -> {
            if (isEdit) {
                onClickYesPopup(true, salary, salSource, salAmt, salDate.getText().toString(), rdGrp1, rdGrp2);
            } else {
                onClickYesPopup(false, salary, salSource, salAmt, date, rdGrp1, rdGrp2);
            }
            popupWindow.dismiss();
        });

        popupWindow.setFocusable(true);
        popupWindow.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        popupWindow.setHeight(WindowManager.LayoutParams.MATCH_PARENT);
        popupWindow.setBackgroundDrawable(null);
        popupWindow.setElevation(6);
        popupWindow.showAsDropDown(addSal);
        AppUtilViewModel appVM = new ViewModelProvider(requireActivity()).get(AppUtilViewModel.class);
        appVM.getCheckerData().observe(requireActivity(), launchChecker -> {
            try {
                if (launchChecker.getTimesLaunched() == 0) {
                    Commons.SnackBar(popupTitle, "To complete fill all the fields and tap on 'Add'");
                    Targets.add(yes);
                }
            } catch (Exception ignored) {
                appVM.InsertLaunchChecker(new LaunchChecker(0));
            }
        });
    }

    private void onClickYesPopup(boolean isEdit, @Nullable salaryEntity salary,
                                 EditText salSrc, EditText salAmt, String salDate,
                                 RadioGroup rdGrp1, RadioGroup rdGrp2) {
//            Insert new value.
        if (!salSrc.getText().toString().trim().isEmpty()) {
            salaryEntity sal = new salaryEntity();
            sal.setCreationDate(salDate);
            sal.setIncName(salSrc.getText().toString());
            if (!isEdit) {
                sal.setSalary(Integer.parseInt(salAmt.getText().toString()));
            } else {
                sal.setSalary(salary.getSalary());
            }
//                Credit Mode.
            switch (rdGrp2.getCheckedRadioButtonId()) {
                case R.id.account:
                    sal.setSalMode(Constants.SAL_MODE_ACC);
                    break;
                case R.id.inHand:
                    sal.setSalMode(Constants.SAL_MODE_CASH);
                    break;
                default:
                    Commons.SnackBar(getView(), "Select a credit mode.");
                    sal = null;
                    break;
            }
//                Income Type.
            try {
                switch (rdGrp1.getCheckedRadioButtonId()) {
                    case R.id.daily:
                        sal.setIncType(Constants.daily);
                        break;
                    case R.id.monthly:
                        sal.setIncType(Constants.monthly);
                        break;
                    case R.id.oneTime:
                        sal.setIncType(Constants.oneTime);
                        break;
                    default:
                        Commons.SnackBar(getView(), "Select a pay period.");
                        sal = null;
                        break;
                }
            } catch (Exception ignored) {
            }
            if (sal != null) {
                if (!isEdit) {
                    if (!salAmt.getText().toString().trim().isEmpty()) {
                        vm.InsertSalary(sal);
                        if (sal.getSalMode() == Constants.SAL_MODE_ACC) {
                            float oldBal = sal.getSalary();
                            if (balanceEntity != null) {
                                oldBal = oldBal + (float) balanceEntity.getBalance();
                                balanceEntity.setBalance((int) oldBal);
                            } else {
                                balanceEntity.setBalance(0);
                            }
                            vm.DeleteBalance();
                            vm.InsertBalance(balanceEntity);
                        } else {
                            float oldBal = sal.getSalary();
                            if (inHandBal != null) {
                                oldBal = oldBal + (float) inHandBal.getBalance();
                                inHandBal.setBalance((int) oldBal);
                            } else {
                                inHandBal.setBalance(0);
                            }
                            vm.DeleteInHandBalance();
                            vm.InsertInHandBalance(inHandBal);
                        }
                    } else {
                        Commons.SnackBar(getView(), "Field(s) may be empty");
                    }
                } else {
                    sal.setId(salary.getId());
                    vm.UpdateSalary(sal);
                }
                appChecker();
            }
        } else {
            Commons.SnackBar(getView(), "Field(s) may be empty");
        }
    }

    private void appChecker() {
        try {
            appVm.getCheckerData().observe(requireActivity(), launchChecker -> {
                if (launchChecker.getTimesLaunched() == 0) {
                    util.setPhaseStatus(1);
                    util.isPhaseStatus().observe(requireActivity(), aBoolean -> {
                        if (aBoolean == 1) {
                            MainActivity.initTutorialPhase3();
                            util.isPhaseStatus().removeObservers(requireActivity());
                        }
                    });
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void callOnDeletePopup(salaryEntity salaryEntity, @Nullable String isUpdate) {
//        "1" = Update Balance
//        "0" = Update Budget
        PopupWindow popupWindow = new PopupWindow(getContext());
        LayoutInflater inflater = (LayoutInflater) requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        View v = inflater.inflate(R.layout.popup_action_expdelete, null);
        popupWindow.setContentView(v);

        Button yes = v.findViewById(R.id.yes_btn);
        Button no = v.findViewById(R.id.no_btn);
        no.setOnClickListener(v1 -> popupWindow.dismiss());
        TextView body = v.findViewById(R.id.edit);
//        CheckBox checkBox = v.findViewById(R.id.check);

        if (salaryEntity != null && isUpdate == null) {
            String s = "This Source will be deleted.";
            body.setText(s);

            yes.setOnClickListener(v1 -> {
                vm.DeleteSalary(salaryEntity);
                popupWindow.dismiss();
                callOnDeletePopup(salaryEntity, "1");
            });
        } else if (salaryEntity != null) {
            if (isUpdate.equals("1")) {
                String s = "Do you want to update balance according to current deletion?";
                body.setText(s);
                yes.setOnClickListener(v1 -> {
                    int type = salaryEntity.getSalMode();
                    int salary = salaryEntity.getSalary();
                    if (type == Constants.SAL_MODE_ACC) {
                        balanceEntity bal = getBal();
                        float curBal = (float) bal.getBalance();
                        vm.DeleteBalance();
                        vm.InsertBalance(new balanceEntity((int) curBal - salary));
                    } else {
                        inHandBalEntity bal = getInHandBal();
                        float curBal = (float) bal.getBalance();
                        vm.DeleteInHandBalance();
                        vm.InsertInHandBalance(new inHandBalEntity((int) curBal - salary));
                    }
                    popupWindow.dismiss();
                    if (budType == Constants.BUDGET_MONTHLY || budType == Constants.BUDGET_WEEKLY) {
                        callOnDeletePopup(salaryEntity, "0");
                    }

                });
            } else if (isUpdate.equals("0") &&
                    (budType == Constants.BUDGET_MONTHLY || budType == Constants.BUDGET_WEEKLY)) {
                String s = "Do you want to update budget according to current deletion?";
                body.setText(s);
                yes.setOnClickListener(v1 -> {
                    Commons.setDefaultBudget(vm, totalSalary, totalExp, budType, budDate);
                    popupWindow.dismiss();
                });
            } else {
                no.callOnClick();
            }
        }

        popupWindow.setFocusable(true);
        popupWindow.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        popupWindow.setHeight(WindowManager.LayoutParams.MATCH_PARENT);
        popupWindow.setBackgroundDrawable(null);
        popupWindow.setElevation(6);
        popupWindow.showAsDropDown(addSal);
    }

    private void touchHelper() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT
                | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                budDate = getBudDate();
                budType = getBudType();
                callOnDeletePopup(adapter.getSalaryEntity(viewHolder.getPosition()), null);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.theme_red))
                        .addActionIcon(R.drawable.common_icon_trash)
                        .addCornerRadius(TypedValue.COMPLEX_UNIT_SP, 15)
                        .addSwipeLeftLabel("Delete")
                        .setSwipeLeftLabelColor(ContextCompat.getColor(requireActivity(), R.color.full_white))
                        .setSwipeLeftLabelTextSize(TypedValue.COMPLEX_UNIT_SP, 12)
                        .addCornerRadius(TypedValue.COMPLEX_UNIT_SP, 15)
                        .addSwipeRightLabel("Delete")
                        .setSwipeRightLabelColor(ContextCompat.getColor(requireActivity(), R.color.full_white))
                        .setSwipeRightLabelTextSize(TypedValue.COMPLEX_UNIT_SP, 12)
                        .create()
                        .decorate();
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(salSplitList);

        adapter.setOnItemClickListener(exp -> {
            inHandBal = getInHandBal();
            balanceEntity = getBal();
            callPopUpWindow(true, exp);
        });
    }

    private void TutorialPhase2() {
//        Floating Btn--
        Targets.add(addSal);
        PrimaryTexts.add("Earnings Tracker Features");
        SecondaryTexts.add("1. Earnings can be added as either cash or bank as per how you received them\n\n2. " +
                "Recurring earnings can be added with their recurrence period so that it will be added to your balance automatically on the respected period\n\n" +
                "3. Once earnings added you can edit them by clicking on each item\n\n4. Earnings can be deleted by swiping left or right on the item\n\n\nTAP ON THE ICON TO ADD A SAMPLE");
//        --------------
        util.TutorialPhase2(Targets, PrimaryTexts, SecondaryTexts);
    }

    private balanceEntity getBal() {
        AtomicReference<balanceEntity> bal = new AtomicReference<>(new balanceEntity());
        vm.getBalance().observe(requireActivity(), balanceEntity -> {
            if (balanceEntity != null) {
                bal.set(balanceEntity);
            } else {
                bal.set(new balanceEntity(0));
            }
        });
        return bal.get();
    }

    private inHandBalEntity getInHandBal() {
        AtomicReference<inHandBalEntity> bal = new AtomicReference<>(new inHandBalEntity());
        vm.getInHandBalance().observe(requireActivity(), inHandBalEntity -> {
            if (inHandBalEntity != null) {
                bal.set(inHandBalEntity);
            } else {
                bal.set(new inHandBalEntity(0));
            }
        });
        return bal.get();
    }

    private int getSalary() {
        AtomicInteger finalTotalSalary = new AtomicInteger();
        vm.getSalary().observe(requireActivity(), entity -> {
            int total = 0;
            if (entity != null) {
                adapter.setSal(entity, Currency);
                accAmt = 0;
                accCount = 0;
                cashAmt = 0;
                cashCount = 0;
                for (int i = 0; i < entity.size(); i++) {
                    total = total + entity.get(i).getSalary();
                    if (entity.get(i).getSalMode() == Constants.SAL_MODE_ACC) {
                        accCount = accCount + 1;
                        accAmt = accAmt + entity.get(i).getSalary();
                    } else {
                        cashCount = cashCount + 1;
                        cashAmt = cashAmt + entity.get(i).getSalary();
                    }
                }
                finalTotalSalary.set(total);
                try {
                    String s1 = Currency + "" + total;
                    SalAmt.setText(s1);
                    inHandCount.setText(String.valueOf(cashCount));
                    accountCount.setText(String.valueOf(accCount));
                    String s2 = Currency + "" + accAmt;
                    accountAmt.setText(s2);
                    String s3 = Currency + "" + cashAmt;
                    inHandAmt.setText(s3);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                finalTotalSalary.set(0);
            }
            totalSalary = total;
        });
        return finalTotalSalary.get();
    }

    private void getExp() {
        vm.getExp().observe(requireActivity(), expEntities -> {
            int total = 0;
            if (expEntities != null) {
                for (int i = 0; i < expEntities.size(); i++) {
                    total = total + expEntities.get(i).getExpenseAmt();
                }
            }
            totalExp = total;
        });
    }

    private int getBudType() {
        AtomicInteger type = new AtomicInteger(3);
        vm.getBudget().observe(requireActivity(), budgetEntity -> {
            try {
                type.set(budgetEntity.getRefreshPeriod());
            } catch (Exception ignored) {
                type.set(3);
            }
        });
        return type.get();
    }

    private String getBudDate() {
        AtomicReference<String> date = new AtomicReference<>("null");
        vm.getBudget().observe(requireActivity(), budgetEntity -> {
            try {
                date.set(budgetEntity.getCreationDate());
            } catch (Exception ignored) {
                date.set("null");
            }
        });
        return date.get();
    }
}