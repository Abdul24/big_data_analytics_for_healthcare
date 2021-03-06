import pandas as pd
from datetime import datetime
from datetime import timedelta
from sklearn.datasets import load_svmlight_file

# PLEASE USE THE GIVEN FUNCTION NAME, DO NOT CHANGE IT

def read_csv(filepath):
    #Columns in events.csv - patient_id,event_id,event_description,timestamp,value
    events_df = pd.read_csv(filepath + 'events.csv', parse_dates=['timestamp'])
    events_df = events_df.sort_values('timestamp')

    #Columns in mortality_event.csv - patient_id,timestamp,label
    try:
        mortality_df = pd.read_csv(filepath + 'mortality_events.csv', parse_dates=['timestamp'])
        mortality_df = mortality_df.sort_values('timestamp')
    except IOError:
        mortality_df = None

    #Columns in event_feature_map.csv - idx,event_id
    try:
        feature_map_df = pd.read_csv(filepath + 'event_feature_map.csv')
    except IOError:
        feature_map_df = None

    return events_df, mortality_df, feature_map_df

def split_dead_and_alive(events, mortality):
    patient_ids = events.patient_id.unique()
    dead_ids = mortality.patient_id
    alive_ids = pd.Series(list(set(patient_ids).difference(set(dead_ids))))

    alive_events = events[events.patient_id.isin(alive_ids)]
    dead_events =  events[events.patient_id.isin(dead_ids)]

    return dead_events, alive_events

def date_offset(x,no_days):
    return datetime.strptime(x, '%Y-%m-%d') + timedelta(days=no_days)

def date_convert(x):
    return datetime.strptime(x, '%Y-%m-%d')

def bag_to_svmlight(input):
    return ' '.join(( "%d:%f" % (fid, float(fvalue)) for fid, fvalue in input))

#input: features and label stored in the svmlight_file
#output: X_train, Y_train
#Note: If the number of features exceed 3190, please use the appropriate number
def get_data_from_svmlight(svmlight_file):
    data_train = load_svmlight_file(svmlight_file,n_features=3190)
    X_train = data_train[0]
    Y_train = data_train[1]
    return X_train, Y_train


def generate_kaggle_submission(svmlight_with_ids_file, Y_pred):
    f = open(svmlight_with_ids_file)
    lines = f.readlines()
    target = open('../my_kaggle_predictions.csv', 'w')
    target.write("%s,%s\n" %("patient_id","label"));
    for i in range(len(lines)):
        target.write("%s,%s\n" %(str(lines[i].split()[0]),str(Y_pred[i])));


def generate_submission(svmlight_with_ids_file, Y_pred):
    f = open(svmlight_with_ids_file)
    lines = f.readlines()
    target = open('../deliverables/my_predictions.csv', 'w')
    target.write("%s,%s\n" %("patient_id","label"));
    for i in range(len(lines)):
        target.write("%s,%s\n" %(str(lines[i].split()[0]),str(Y_pred[i])));
