import xml.etree.ElementTree as ET
import re

files_map = {
    'HerdDataIcon': 'app/src/main/res/drawable/ic_herd_data.xml',
    'FeedManagementIcon': 'app/src/main/res/drawable/ic_feed2.xml',
    'HerdActivitiesIcon': 'app/src/main/res/drawable/ic_herd_activities.xml',
    'SymptomsAnalyzerIcon': 'app/src/main/res/drawable/ic_symptoms_analyzer.xml',
    'WeightCheckerIcon': 'app/src/main/res/drawable/ic_weight_checker.xml',
}

def get_paths(xml_path):
    tree = ET.parse(xml_path)
    root = tree.getroot()
    paths_jsx = []
    for elem in root.iter():
        if elem.tag.endswith('path'):
            path_data = elem.attrib.get('{http://schemas.android.com/apk/res/android}pathData', '')
            fill_type = elem.attrib.get('{http://schemas.android.com/apk/res/android}fillType', '')
            if not path_data:
                continue
            fill_rule = ' fillRule="evenodd" clipRule="evenodd"' if fill_type == 'evenOdd' else ''
            jsx = f'    <path{fill_rule} d="{path_data}" />'
            paths_jsx.append(jsx)
    return "\n".join(paths_jsx)

with open('web/src/components/icons/DashboardIcons.tsx', 'r') as f:
    content = f.read()

for icon_name, xml_file in files_map.items():
    paths = get_paths(xml_file)
    pattern = rf'(export const {icon_name} =.*?<svg[^>]*>)(.*?)(</svg>)'
    
    def replacer(match):
        return match.group(1) + '\n' + paths + '\n  ' + match.group(3)
        
    content = re.sub(pattern, replacer, content, flags=re.DOTALL)

with open('web/src/components/icons/DashboardIcons.tsx', 'w') as f:
    f.write(content)
