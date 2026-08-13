export interface UserSettings {
  weaningDays: string;
  farrowingDays: string;
  ironDay1: string;
  ironDay2: string;
  autoClassifyBarrows: boolean;
  autoClassifySows: boolean;
  notificationsEnabled: boolean;
  selectedCurrency: string;
  currencySymbol: string;
  giltAgeThresholdWeeks: string;
  porkerUseAge: boolean;
  porkerStarterAge: string;
  porkerGrowerAge: string;
  porkerStarterWeight: string;
  porkerGrowerWeight: string;
  breederUseAge: boolean;
  breederPigletAge: string;
  breederWeanerAge: string;
  breederGrowerAge: string;
  breederPigletWeight: string;
  breederWeanerWeight: string;
  breederGrowerWeight: string;
}

export interface UserProfile {
  firstName: string;
  lastName: string;
  farmName: string;
  country: string;
  countryCode?: string;
  email: string;
  isPremium: boolean;
  isAdmin: boolean;
  isKofisPerson: boolean;
  subscriptionSource?: string;
  appLanguage?: string;
  settings: UserSettings;
}

export interface Pig {
  id: string;
  tagNumber: string;
  birthDate: string;
  breed: string;
  gender: string;
  weight: number;
  lastWeightDate?: string;
  purpose: string;
  sowTag: string;
  boarTag: string;
  location: string;
  source: string;
  status: string;
  notes: string;
  // Health & Parity with Android
  isCastrated?: boolean | null;
  isTeethClipped?: boolean;
  isTailDocked?: boolean;
  isWeaned?: boolean;
  weaned?: boolean;
  castrated?: boolean | null;
  teethClipped?: boolean;
  tailDocked?: boolean;
  ironInjections?: number;
  castrationDate?: string;
  lastBreedingDate?: string;
  lastBoarTag?: string;
  hasFarrowed?: boolean;
  healthRecords?: HealthRecord[];
}

export interface HealthRecord {
  id: string;
  date: string;
  type: string;
  description: string;
  medication?: string;
  cost?: number;
  taskId?: string;
}

export interface TaskItem {
  id: string;
  name: string;
  date: string;
  notes: string;
  pigIds: string[];
  completed?: boolean;
  healthRecordIds?: string[];
}

export interface FeedIngredient {
  id: string;
  name: string;
  crudeProtein: number;
  crudeFiber: number;
  calcium: number;
  phosphorus: number;
  sodium: number;
  chloride: number;
  potassium: number;
  sulfur: number;
  metabolizableEnergy: number; // ME (kcal/kg)
  dryMatter: number;
  fat: number;
  lysine: number;
  methionine: number;
  cystine: number;
  threonine: number;
  tryptophan: number;
  arginine: number;
  isoleucine?: number;
  valine?: number;
  category: string;
  description: string;
  quantity: number;
  unit: string;
  costPerKg: number;
  mainCategory: string;
  visible: boolean;
  maxStarter: number;
  maxGrower: number;
  maxFinisher: number;
}

export interface NutritionalRequirement {
  stage: string;
  digestibleProtein: number; // as %
  metabolizableEnergy: number; // ME (kcal/kg)
  calcium: number; // as %
  phosphorus: number; // as %
  lysine: number; // as % ptn
  methionineCystine: number; // as % ptn
  tryptophan: number; // as % ptn
  crudeFiber: number; // as %
  minDailyFeed: number; // kg/day
  maxDailyFeed: number; // kg/day
}

export interface FinancialRecord {
  id: string;
  date: string;
  type: string; // "Income" | "Expense"
  category: string;
  amount: number;
  description: string;
  pigId?: string;
}

export interface StaffMember {
  id: string;
  name: string;
  role: string;
  phone: string;
  salary: number;
  joinDate: string;
  status: string; // "Active", "Inactive", "On Leave"
  allowAppAccess: boolean;
  email: string;
  inviteStatus?: string; // "none", "pending", "sent", "failed"
}

export interface FeedInventoryItem {
  id: string;
  name: string;
  feedType: string;
  quantity: number;
  unit: string;
  unitWeight: number;
  minThreshold: number;
  costPerUnit: number;
  lastUpdated: string;
}

export interface FeedInventoryTransaction {
  id: string;
  itemId: string;
  itemName: string;
  type: string; // "Restock" or "Usage"
  quantity: number;
  unit: string;
  cost: number;
  date: string;
  notes: string;
}
