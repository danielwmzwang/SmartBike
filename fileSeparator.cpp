#include <iostream>
#include <fstream>
#include <string>
#include <map>
using namespace std;

int main(int argc, char** argv) {
    ifstream is;
    string sa;

    ofstream rpm;
    rpm.open("rpms.txt");
    ofstream speed;
    speed.open("speeds.txt");
    ofstream distance;
    distance.open("distances.txt");
    ofstream crpm;
    crpm.open("crpms.txt");
    ofstream power;
    power.open("powers.txt");
    ofstream incline;
    incline.open("inclines.txt");

    string rpms;
    string speeds;
    string distances;
    string crpms;
    string powers;
    string inclines;

    is.open(argv[1]);

    map<char,string> mappings;
    mappings['r'] = rpms;
    mappings['s'] = speeds;
    mappings['d'] = distances;
    mappings['c'] = crpms;
    mappings['p'] = powers;
    mappings['i'] = inclines;

    while (getline(is, sa)) {
        mappings[sa[0]].append(sa.append("\n"));
    }

    rpm << mappings['r'];
    speed << mappings['s'];
    distance << mappings['d'];
    crpm << mappings['c'];
    power << mappings['p'];
    incline << mappings['i'];

    // Close files
    is.close();
    rpm.close();
    speed.close();
    distance.close();
    crpm.close();
    power.close();
    incline.close();

    return 0;
}