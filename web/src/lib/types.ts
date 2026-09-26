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
  lemonSqueezyCustomerId?: string;
  lemonSqueezySubscriptionId?: string;
  lemonSqueezyVariantId?: string;
  lemonSqueezyCustomerPortalUrl?: string;
  lemonSqueezyRenewsAt?: string;
  lemonSqueezyEndsAt?: string;
  paystackCustomerCode?: string;
  paystackSubscriptionCode?: string;
  paystackPlanCode?: string;
  subscriptionPlan?: string;
  subscriptionUpdatedAt?: string;
  appLanguage?: string;
  farmLogo?: string;
  photoURL?: string;
  isFinancialsRestricted?: boolean;
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
  archived?: boolean;
  activeWithdrawalUntil?: string;
  withdrawalMedication?: string;
  withdrawalPeriodDays?: number;
  expectedFarrowingDate?: string;
  farrowingPenMoveDate?: string;
  parity?: number;
  healthRecords?: HealthRecord[];
}

export interface HealthRecord {
  id: string;
  date: string;
  type: string;
  description: string;
  treatment?: string;
  medication?: string;
  dosage?: string;
  cost?: number;
  taskId?: string;
  weight?: number;
  activeWithdrawalUntil?: string;
  withdrawalMedication?: string;
  withdrawalPeriodDays?: number;
  safeSlaughterDate?: string;
  stillbornCount?: number;
  mummiesCount?: number;
  litterBirthWeightKg?: number;
}

export interface TaskItem {
  id: string;
  name: string;
  date: string;
  notes: string;
  pigIds: string[];
  completed?: boolean;
  isArchived?: boolean;
  healthRecordIds?: string[];
  snoozeUntil?: number;
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
  crudeProtein?: number; // as %
  dietaryLysine?: number;
  dietaryMethionine?: number;
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

export interface SavedFeedRecipe {
  id: string;
  name: string;
  stage: string;
  dateCreated: string;
  ingredients: Record<string, number>;
  isPercentage: boolean;
  costPerKg: number;
  targetBatchKg: number;
  notes: string;
  timestamp: number;
}

export interface FinancialRecord {
  id: string;
  date: string;
  type: string; // "Income" | "Expense"
  category: string;
  amount: number;
  description: string;
  pigId?: string;
  pigIds?: string[];
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
  isFinancialsRestricted?: boolean;
  email: string;
  inviteStatus?: string; // "none", "pending", "sent", "failed"
  gender?: string;
  residentialAddress?: string;
  dateOfBirth?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactAddress?: string;
  emergencyContactRelation?: string;
  photoUrl?: string;
  createdAt?: number | string;
  updatedAt?: number | string;
}

export interface FeedInventoryItem {
  id: string;
  name: string;
  feedType: string;
  itemCategory?: string; // "Complete Feed" | "Feed Ingredient"
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
